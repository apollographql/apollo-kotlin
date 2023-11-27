package com.apollographql.ijplugin.normalizedcache

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.apollodebugserver.normalizedCacheSimpleName
import com.apollographql.ijplugin.normalizedcache.NormalizedCacheSource.ApolloDebugServer
import com.apollographql.ijplugin.normalizedcache.NormalizedCacheSource.DeviceFile
import com.apollographql.ijplugin.normalizedcache.NormalizedCacheSource.LocalFile
import com.apollographql.ijplugin.normalizedcache.provider.ApolloDebugNormalizedCacheProvider
import com.apollographql.ijplugin.normalizedcache.provider.DatabaseNormalizedCacheProvider
import com.apollographql.ijplugin.normalizedcache.ui.FieldTreeTable
import com.apollographql.ijplugin.normalizedcache.ui.RecordSearchTextField
import com.apollographql.ijplugin.normalizedcache.ui.RecordTable
import com.apollographql.ijplugin.telemetry.TelemetryEvent.ApolloIjNormalizedCacheOpenApolloDebugCache
import com.apollographql.ijplugin.telemetry.TelemetryEvent.ApolloIjNormalizedCacheOpenDeviceFile
import com.apollographql.ijplugin.telemetry.TelemetryEvent.ApolloIjNormalizedCacheOpenLocalFile
import com.apollographql.ijplugin.telemetry.telemetryService
import com.apollographql.ijplugin.util.logw
import com.apollographql.ijplugin.util.showNotification
import com.intellij.icons.AllIcons
import com.intellij.ide.CommonActionsManager
import com.intellij.ide.dnd.FileCopyPasteUtil
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ex.ToolWindowEx
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBPanelWithEmptyText
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManager
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.runBlocking
import org.sqlite.SQLiteException
import java.awt.BorderLayout
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetAdapter
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetDropEvent
import java.awt.dnd.DropTargetEvent
import java.awt.event.KeyEvent
import java.io.File
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities

class NormalizedCacheToolWindowFactory : ToolWindowFactory, DumbAware, Disposable {
  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    val newTabAction = object : DumbAwareAction(ApolloBundle.messagePointer("normalizedCacheViewer.newTab"), AllIcons.General.Add) {
      override fun actionPerformed(e: AnActionEvent) {
        createNewTab(project, toolWindow.contentManager)
      }
    }.apply {
      registerCustomShortcutSet(KeyEvent.VK_T, KeyEvent.CTRL_DOWN_MASK, toolWindow.component)
    }
    (toolWindow as ToolWindowEx).setTabActions(newTabAction)

    // Open a new tab if all tabs were closed
    project.messageBus.connect().subscribe(
        ToolWindowManagerListener.TOPIC,
        object : ToolWindowManagerListener {
          override fun toolWindowShown(shownToolWindow: ToolWindow) {
            if (toolWindow === shownToolWindow && toolWindow.isVisible && toolWindow.contentManager.isEmpty) {
              createNewTab(project, toolWindow.contentManager)
            }
          }
        }
    )

    createNewTab(project, toolWindow.contentManager)
  }

  private fun createNewTab(project: Project, contentManager: ContentManager) {
    contentManager.addContent(
        ContentFactory.getInstance().createContent(
            NormalizedCacheWindowPanel(project) { tabName -> contentManager.selectedContent?.displayName = tabName },
            ApolloBundle.message("normalizedCacheViewer.tabName.empty"),
            false
        )
    )
    contentManager.setSelectedContent(contentManager.contents.last())
  }

  override fun dispose() {}
}

class NormalizedCacheWindowPanel(
    private val project: Project,
    private val setTabName: (tabName: String) -> Unit,
) : SimpleToolWindowPanel(false, true), Disposable {
  private lateinit var normalizedCache: NormalizedCache

  private lateinit var recordTable: RecordTable
  private lateinit var recordSearchTextField: RecordSearchTextField

  private lateinit var fieldTreeTable: FieldTreeTable

  private val history = History<NormalizedCache.Record>()
  private var updateHistory = true

  private var cacheSource: NormalizedCacheSource? = null
  private var isRefreshing = false

  init {
    setContent(createEmptyContent())
  }

  private fun createEmptyContent(): JComponent {
    return JBPanelWithEmptyText().apply {
      emptyText.text = ApolloBundle.message("normalizedCacheViewer.empty.message")
      emptyText.appendLine(ApolloBundle.message("normalizedCacheViewer.empty.openFile"), SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES) {
        pickFile()
      }
      if (isAndroidPluginPresent) {
        emptyText.appendLine(ApolloBundle.message("normalizedCacheViewer.empty.pullFromDevice"), SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES) {
          PullFromDeviceDialog(
              project = project,
              onSourceSelected = ::openNormalizedCache,
          ).show()
        }
      }

      val defaultBackground = background
      dropTarget = DropTarget(this, DnDConstants.ACTION_COPY, object : DropTargetAdapter() {
        override fun dragEnter(e: DropTargetDragEvent) {
          if (FileCopyPasteUtil.isFileListFlavorAvailable(e.currentDataFlavors)) {
            e.acceptDrag(DnDConstants.ACTION_COPY)
            background = JBUI.CurrentTheme.DragAndDrop.Area.BACKGROUND
          } else {
            e.rejectDrag()
          }
        }

        override fun dragOver(e: DropTargetDragEvent) {
          dragEnter(e)
        }

        override fun dropActionChanged(e: DropTargetDragEvent) {
          dragEnter(e)
        }

        override fun dragExit(dte: DropTargetEvent?) {
          background = defaultBackground
        }

        override fun drop(e: DropTargetDropEvent) {
          e.acceptDrop(DnDConstants.ACTION_COPY)
          val paths = FileCopyPasteUtil.getFileList(e.transferable)
          if (!paths.isNullOrEmpty()) {
            val file = paths.first()
            openNormalizedCache(LocalFile(file))
            e.dropComplete(true)
          } else {
            e.dropComplete(false)
          }
          background = defaultBackground
        }
      })
    }
  }

  private fun createLoadingContent(): JComponent {
    return JBPanelWithEmptyText().apply {
      emptyText.text = ApolloBundle.message("normalizedCacheViewer.loading.message")
    }
  }

  private fun createToolbar(): JComponent {
    val group = DefaultActionGroup().apply {
      add(object : DumbAwareAction(ApolloBundle.messagePointer("normalizedCacheViewer.toolbar.back"), AllIcons.Actions.Back) {
        init {
          ActionUtil.copyFrom(this, IdeActions.ACTION_GOTO_BACK)
          registerCustomShortcutSet(this.shortcutSet, this@NormalizedCacheWindowPanel)
        }

        override fun actionPerformed(e: AnActionEvent) {
          val record = history.back() ?: return
          updateHistory = false
          selectRecord(record.key)
        }

        override fun update(e: AnActionEvent) {
          e.presentation.isEnabled = history.canGoBack()
        }

        override fun getActionUpdateThread() = ActionUpdateThread.BGT
      })
      add(object : DumbAwareAction(ApolloBundle.messagePointer("normalizedCacheViewer.toolbar.forward"), AllIcons.Actions.Forward) {
        init {
          ActionUtil.copyFrom(this, IdeActions.ACTION_GOTO_FORWARD)
          registerCustomShortcutSet(this.shortcutSet, this@NormalizedCacheWindowPanel)
        }

        override fun actionPerformed(e: AnActionEvent) {
          val record = history.forward() ?: return
          updateHistory = false
          selectRecord(record.key)
        }

        override fun update(e: AnActionEvent) {
          e.presentation.isEnabled = history.canGoForward()
        }

        override fun getActionUpdateThread() = ActionUpdateThread.BGT
      })
      addSeparator()
      add(CommonActionsManager.getInstance().createExpandAllAction(fieldTreeTable.treeExpander, this@NormalizedCacheWindowPanel).apply {
        getTemplatePresentation().setDescription(ApolloBundle.message("normalizedCacheViewer.toolbar.expandAll"))
      })
      add(CommonActionsManager.getInstance().createCollapseAllAction(fieldTreeTable.treeExpander, this@NormalizedCacheWindowPanel).apply {
        getTemplatePresentation().setDescription(ApolloBundle.message("normalizedCacheViewer.toolbar.collapseAll"))
      })
      addSeparator()
      add(object : DumbAwareAction(ApolloBundle.messagePointer("normalizedCacheViewer.toolbar.refresh"), AllIcons.Actions.Refresh) {
        init {
          ActionUtil.copyFrom(this, IdeActions.ACTION_REFRESH)
          registerCustomShortcutSet(this.shortcutSet, this@NormalizedCacheWindowPanel)
        }

        override fun actionPerformed(e: AnActionEvent) {
          cacheSource?.let { openNormalizedCache(it) }
        }

        override fun update(e: AnActionEvent) {
          e.presentation.isVisible = cacheSource != null
          e.presentation.isEnabled = !isRefreshing
        }

        override fun getActionUpdateThread() = ActionUpdateThread.BGT
      })

    }

    val actionToolBar = ActionManager.getInstance().createActionToolbar(ActionPlaces.TOOLBAR, group, false)
    actionToolBar.targetComponent = this
    return actionToolBar.component
  }

  private fun createNormalizedCacheContent(): JComponent {
    fieldTreeTable = FieldTreeTable(::selectRecord)
    val recordTableWithFilter = createRecordTableWithFilter()
    val splitter = OnePixelSplitter(false, .25F).apply {
      firstComponent = recordTableWithFilter
      secondComponent = fieldTreeTable
      setResizeEnabled(true)
      splitterProportionKey = "${NormalizedCacheToolWindowFactory::class.java}.splitterProportionKey"
    }
    SwingUtilities.invokeLater {
      recordTable.requestFocusInWindow()
    }
    return splitter
  }

  private fun createRecordTableWithFilter(): JComponent {
    recordTable = RecordTable(normalizedCache).apply {
      selectionModel.addListSelectionListener {
        if (selectedRow == -1) return@addListSelectionListener
        val selectedRowAfterSort = convertRowIndexToModel(selectedRow)
        model.getRecordAt(selectedRowAfterSort)?.let { record ->
          fieldTreeTable.setRecord(record)
          if (!updateHistory) {
            updateHistory = true
          } else {
            history.push(record)
          }
        }
      }
      selectionModel.setSelectionInterval(0, 0)
    }

    recordSearchTextField = RecordSearchTextField(recordTable)
    recordTable.addKeyListener(recordSearchTextField)

    val tableWithFilter = JPanel(BorderLayout()).apply {
      add(recordSearchTextField, BorderLayout.NORTH)
      add(ScrollPaneFactory.createScrollPane(recordTable), BorderLayout.CENTER)
    }

    return tableWithFilter
  }

  private fun selectRecord(key: String) {
    if (!recordTable.model.isRecordShowing(key)) {
      // Filtered list doesn't contain the record, so clear the filter
      recordSearchTextField.text = ""
    }
    recordTable.selectRecord(key)
    recordTable.requestFocusInWindow()
  }

  private fun pickFile() {
    val virtualFile = FileChooser.chooseFiles(
        FileChooserDescriptor(true, false, false, false, false, false),
        project,
        null
    ).firstOrNull() ?: return
    openNormalizedCache(LocalFile(File(virtualFile.path)))
  }

  private fun openNormalizedCache(cacheSource: NormalizedCacheSource) {
    project.telemetryService.logEvent(when (cacheSource) {
      is ApolloDebugServer -> ApolloIjNormalizedCacheOpenApolloDebugCache()
      is DeviceFile -> ApolloIjNormalizedCacheOpenDeviceFile()
      is LocalFile -> ApolloIjNormalizedCacheOpenLocalFile()
    })
    setContent(createLoadingContent())
    object : Task.Backgroundable(
        project,
        ApolloBundle.message("normalizedCacheViewer.loading.message"),
        false,
    ) {
      override fun run(indicator: ProgressIndicator) {
        isRefreshing = true
        var tabName = ""
        val normalizedCacheResult = when (cacheSource) {
          is LocalFile -> {
            runBlocking {
              DatabaseNormalizedCacheProvider().provide(cacheSource.file).also {
                tabName = cacheSource.file.name
              }
            }
          }

          is DeviceFile -> {
            runBlocking {
              pullFile(
                  device = cacheSource.device,
                  appPackageName = cacheSource.packageName,
                  remoteDirName = cacheSource.remoteDirName,
                  remoteFileName = cacheSource.remoteFileName,
              ).mapCatching { file ->
                tabName = cacheSource.remoteFileName
                DatabaseNormalizedCacheProvider().provide(file).getOrThrow()
              }
            }
          }

          is ApolloDebugServer -> {
            runBlocking {
              cacheSource.apolloDebugClient.getNormalizedCache(apolloClientId = cacheSource.apolloClientId, normalizedCacheId = cacheSource.normalizedCacheId)
            }.mapCatching { apolloDebugNormalizedCache ->
              val tabNamePrefix = cacheSource.apolloClientId.takeIf { it != "client" }?.let { "$it - " } ?: ""
              tabName = tabNamePrefix + apolloDebugNormalizedCache.displayName.normalizedCacheSimpleName
              ApolloDebugNormalizedCacheProvider().provide(apolloDebugNormalizedCache).getOrThrow()
            }
          }
        }
        isRefreshing = false
        invokeLater {
          if (normalizedCacheResult.isFailure) {
            showOpenFileError(normalizedCacheResult.exceptionOrNull()!!)
            if (this@NormalizedCacheWindowPanel.cacheSource == null) {
              setContent(createEmptyContent())
            }
            return@invokeLater
          }
          this@NormalizedCacheWindowPanel.cacheSource = cacheSource
          normalizedCache = normalizedCacheResult.getOrThrow().sorted()
          setContent(createNormalizedCacheContent())
          if (toolbar != null) toolbar = null
          toolbar = createToolbar()
          setTabName(tabName)
        }
      }
    }.queue()
  }

  private fun showOpenFileError(exception: Throwable) {
    logw(exception, "Could not open file")
    val details = when (exception) {
      is SQLiteException -> exception.resultCode.message
      else -> exception.message ?: exception.javaClass.simpleName
    }
    showNotification(project, title = ApolloBundle.message("normalizedCacheViewer.openFileError.title"), content = details, type = NotificationType.ERROR)
  }

  override fun dispose() {
    (cacheSource as? ApolloDebugServer)?.apolloDebugClient?.close()
  }
}
