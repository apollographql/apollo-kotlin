package com.apollographql.ijplugin.normalizedcache

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.normalizedcache.NormalizedCache.FieldValue.BooleanValue
import com.apollographql.ijplugin.normalizedcache.NormalizedCache.FieldValue.CompositeValue
import com.apollographql.ijplugin.normalizedcache.NormalizedCache.FieldValue.ListValue
import com.apollographql.ijplugin.normalizedcache.NormalizedCache.FieldValue.Null
import com.apollographql.ijplugin.normalizedcache.NormalizedCache.FieldValue.NumberValue
import com.apollographql.ijplugin.normalizedcache.NormalizedCache.FieldValue.Reference
import com.apollographql.ijplugin.normalizedcache.NormalizedCache.FieldValue.StringValue
import com.apollographql.ijplugin.util.logw
import com.apollographql.ijplugin.util.showNotification
import com.intellij.icons.AllIcons
import com.intellij.ide.CommonActionsManager
import com.intellij.ide.DefaultTreeExpander
import com.intellij.ide.TreeExpander
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
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.JBMenuItem
import com.intellij.openapi.ui.JBPopupMenu
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ex.ToolWindowEx
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.ColoredTableCellRenderer
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.ScrollingUtil
import com.intellij.ui.SearchTextField
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBPanelWithEmptyText
import com.intellij.ui.components.JBTreeTable
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManager
import com.intellij.ui.speedSearch.FilteringListModel
import com.intellij.ui.speedSearch.ListWithFilter
import com.intellij.ui.speedSearch.SpeedSearchUtil
import com.intellij.ui.table.JBTable
import com.intellij.ui.treeStructure.treetable.ListTreeTableModel
import com.intellij.ui.treeStructure.treetable.TreeTableModel
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.ListUiUtil
import com.intellij.util.ui.UIUtil
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.sqlite.SQLiteException
import java.awt.Color
import java.awt.Component
import java.awt.Cursor
import java.awt.MouseInfo
import java.awt.Point
import java.awt.datatransfer.StringSelection
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetAdapter
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetDropEvent
import java.awt.dnd.DropTargetEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.JTable
import javax.swing.ListSelectionModel
import javax.swing.SwingUtilities
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath


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
) : SimpleToolWindowPanel(false, true) {
  private lateinit var normalizedCache: NormalizedCache

  private lateinit var recordList: JBList<NormalizedCache.Record>
  private lateinit var recordListFilter: ListWithFilter<NormalizedCache.Record>

  private lateinit var fieldTreeTableModel: ListTreeTableModel
  private lateinit var fieldTreeExpander: TreeExpander

  private val history = History<NormalizedCache.Record>()
  private var updateHistory = true

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
          object : Task.Backgroundable(
              project,
              ApolloBundle.message("normalizedCacheViewer.loading.message"),
              false,
          ) {
            override fun run(indicator: ProgressIndicator) {
              val pullFromDeviceActionGroup = createPullFromDeviceActionGroup(
                  project,
                  onFilePullError = { throwable ->
                    showNotification(project, title = ApolloBundle.message("normalizedCacheViewer.pullFromDevice.pull.error"), content = throwable.message
                        ?: "", type = NotificationType.ERROR)
                  },
                  onFilePulled = ::openFile
              )
              invokeLater {
                val actionPopupMenu = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.TOOLWINDOW_POPUP, pullFromDeviceActionGroup)
                JBPopupMenu.showAt(RelativePoint(MouseInfo.getPointerInfo().location), actionPopupMenu.component)
              }
            }
          }.queue()
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
            openFile(paths.first())
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
      add(CommonActionsManager.getInstance().createExpandAllAction(fieldTreeExpander, this@NormalizedCacheWindowPanel).apply {
        getTemplatePresentation().setDescription(ApolloBundle.message("normalizedCacheViewer.toolbar.expandAll"))
      })
      add(CommonActionsManager.getInstance().createCollapseAllAction(fieldTreeExpander, this@NormalizedCacheWindowPanel).apply {
        getTemplatePresentation().setDescription(ApolloBundle.message("normalizedCacheViewer.toolbar.collapseAll"))
      })
    }

    val actionToolBar = ActionManager.getInstance().createActionToolbar(ActionPlaces.TOOLBAR, group, false)
    actionToolBar.targetComponent = this
    return actionToolBar.component
  }

  private fun createNormalizedCacheContent(): JComponent {
    val fieldTreeTable = createFieldTreeTable()
    val recordList = createRecordList()
    val splitter = OnePixelSplitter(false, .25F).apply {
      firstComponent = recordList
      secondComponent = fieldTreeTable
      setResizeEnabled(true)
      splitterProportionKey = "${NormalizedCacheToolWindowFactory::class.java}.splitterProportionKey"
    }
    return splitter
  }

  private fun createRecordList(): JComponent {
    val listModel = DefaultListModel<NormalizedCache.Record>().apply {
      addAll(normalizedCache.records)
    }
    recordList = JBList(listModel).apply {
      selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
      ScrollingUtil.installActions(this)
      ListUiUtil.Selection.installSelectionOnFocus(this)
      ListUiUtil.Selection.installSelectionOnRightClick(this)

      cellRenderer = object : ColoredListCellRenderer<NormalizedCache.Record>() {
        override fun customizeCellRenderer(
            list: JList<out NormalizedCache.Record>,
            value: NormalizedCache.Record,
            index: Int,
            selected: Boolean,
            hasFocus: Boolean,
        ) {
          append(value.key)
          SpeedSearchUtil.applySpeedSearchHighlighting(list, this, true, selected)
        }
      }

      addListSelectionListener {
        if (selectedValue != null) {
          fieldTreeTableModel.setRoot(getRootNodeForRecord(selectedValue))
          if (!updateHistory) {
            updateHistory = true
          } else {
            history.push(selectedValue)
          }
        }
      }

      selectedIndex = 0
    }
    @Suppress("UNCHECKED_CAST")
    recordListFilter = ListWithFilter.wrap(
        recordList,
        ScrollPaneFactory.createScrollPane(recordList),
        { it.key },
        true,
        true,
        true,
    ) as ListWithFilter<NormalizedCache.Record>

    // Fix clicking on the search field not focusing the list
    recordListFilter.components.firstIsInstance<SearchTextField>().textEditor.addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        recordList.requestFocusInWindow()
      }
    })
    return recordListFilter
  }

  @Suppress("UnstableApiUsage")
  private fun createFieldTreeTable(): JComponent {
    fieldTreeTableModel = ListTreeTableModel(
        DefaultMutableTreeNode(),
        arrayOf(
            object : ColumnInfo<Unit, Unit>(ApolloBundle.message("normalizedCacheViewer.fields.column.key")) {
              override fun getColumnClass() = TreeTableModel::class.java
              override fun valueOf(item: Unit) = Unit
            },
            object : ColumnInfo<NormalizedCacheFieldTreeNode, NormalizedCache.Field>(ApolloBundle.message("normalizedCacheViewer.fields.column.value")) {
              override fun getColumnClass() = NormalizedCache.Field::class.java
              override fun valueOf(item: NormalizedCacheFieldTreeNode) = item.field
            },
        ),
    )

    val treeTable = object : JBTreeTable(fieldTreeTableModel) {
      override fun getPathBackground(path: TreePath, row: Int): Color? {
        return if (row % 2 == 0) {
          UIUtil.getDecoratedRowColor()
        } else {
          null
        }
      }
    }.apply {
      columnProportion = .8F
      setDefaultRenderer(NormalizedCache.Field::class.java, object : ColoredTableCellRenderer() {
        override fun customizeCellRenderer(table: JTable, value: Any?, selected: Boolean, hasFocus: Boolean, row: Int, column: Int) {
          value as NormalizedCache.Field
          when (val v = value.value) {
            is StringValue -> append("\"${v.value}\"")
            is NumberValue -> append(v.value.toString())
            is BooleanValue -> append(v.value.toString())
            is ListValue -> append(when (val size = v.value.size) {
              0 -> ApolloBundle.message("normalizedCacheViewer.fields.list.empty")
              1 -> ApolloBundle.message("normalizedCacheViewer.fields.list.single")
              else -> ApolloBundle.message("normalizedCacheViewer.fields.list.multiple", size)
            }, SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES)

            is CompositeValue -> append("{...}", SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES)
            Null -> append("null")
            is Reference -> {
              append("→ ", SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES)
              append(v.key, SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES)
            }
          }
        }
      })

      // Handle reference clicks and cursor changes
      val mouseAdapter = object : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent) {
          if (e.button != MouseEvent.BUTTON1) return
          table.cursor = Cursor(Cursor.DEFAULT_CURSOR)
          val field = table.getFieldAtPoint(e) ?: return
          if (field.value is Reference) {
            selectRecord(field.value.key)
          }
        }

        override fun mouseMoved(e: MouseEvent) {
          table.cursor = Cursor(
              if (table.getFieldAtPoint(e)?.value is Reference) {
                Cursor.HAND_CURSOR
              } else {
                Cursor.DEFAULT_CURSOR
              }
          )
        }

        override fun mouseExited(e: MouseEvent) {
          table.cursor = Cursor(Cursor.DEFAULT_CURSOR)
        }
      }
      table.addMouseListener(mouseAdapter)
      table.addMouseMotionListener(mouseAdapter)

      table.isStriped = true

      installPopupMenu(table)

      fieldTreeExpander = DefaultTreeExpander { tree }
    }
    return treeTable
  }

  private fun installPopupMenu(table: JBTable) {
    val popupMenu = object : JBPopupMenu() {
      override fun show(invoker: Component, x: Int, y: Int) {
        when (table.getFieldAtPoint(x, y)?.value) {
          is StringValue,
          is NumberValue,
          is BooleanValue,
          is Reference,
          Null,
          -> {
            super.show(invoker, x, y)
          }

          else -> {}
        }
      }
    }
    popupMenu.add(JBMenuItem(ApolloBundle.message("normalizedCacheViewer.fields.popupMenu.copyValue")).apply {
      addActionListener {
        val field = table.getValueAt(table.selectedRow, table.selectedColumn) as NormalizedCache.Field
        val valueStr = when (val value = field.value) {
          is StringValue -> value.value
          is NumberValue -> value.value.toString()
          is BooleanValue -> value.value.toString()
          is Reference -> value.key
          Null -> "null"
          else -> return@addActionListener
        }
        CopyPasteManager.getInstance().setContents(StringSelection(valueStr))
      }
    })

    // Select the row under the popup menu
    popupMenu.addPopupMenuListener(object : PopupMenuListener {
      override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
        invokeLater {
          val row = table.rowAtPoint(SwingUtilities.convertPoint(popupMenu, Point(0, 0), table))
          val column = table.columnAtPoint(SwingUtilities.convertPoint(popupMenu, Point(0, 0), table))
          table.setRowSelectionInterval(row, row)
          table.setColumnSelectionInterval(column, column)
        }
      }

      override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) {}

      override fun popupMenuCanceled(e: PopupMenuEvent) {}
    })
    table.componentPopupMenu = popupMenu
  }

  private fun JBTable.getFieldAtPoint(x: Int, y: Int): NormalizedCache.Field? {
    val point = Point(x, y)
    val row = rowAtPoint(point)
    // Add 1 to account for the tree column
    val column = columnAtPoint(point) + 1
    return model.getValueAt(row, column) as NormalizedCache.Field?
  }

  private fun JBTable.getFieldAtPoint(e: MouseEvent): NormalizedCache.Field? {
    return getFieldAtPoint(e.x, e.y)
  }

  private fun selectRecord(key: String) {
    val record = normalizedCache.records.firstOrNull { it.key == key } ?: return
    if (!(recordList.model as FilteringListModel).contains(record)) {
      // Filtered list doesn't contain the record, so clear the filter
      recordListFilter.resetFilter()
    }
    recordList.setSelectedValue(record, true)
  }

  private fun getRootNodeForRecord(record: NormalizedCache.Record) = DefaultMutableTreeNode().apply {
    addFields(record.fields)
  }

  private fun DefaultMutableTreeNode.addFields(fields: List<NormalizedCache.Field>) {
    for (field in fields) {
      val childNode = NormalizedCacheFieldTreeNode(field)
      add(childNode)
      when (val value = field.value) {
        is ListValue -> childNode.addFields(value.value.mapIndexed { i, v -> NormalizedCache.Field(i.toString(), v) })
        is CompositeValue -> childNode.addFields(value.value)
        else -> {}
      }
    }
  }

  private fun pickFile() {
    val virtualFile = FileChooser.chooseFiles(
        FileChooserDescriptor(true, false, false, false, false, false)
            .withFileFilter { it.extension == "db" },
        project,
        null
    ).firstOrNull() ?: return
    openFile(File(virtualFile.path))
  }

  private fun openFile(file: File) {
    setContent(createLoadingContent())
    object : Task.Backgroundable(
        project,
        ApolloBundle.message("normalizedCacheViewer.loading.message"),
        false,
    ) {
      override fun run(indicator: ProgressIndicator) {
        val normalizedCacheResult = DatabaseNormalizedCacheProvider().provide(file)
        invokeLater {
          if (normalizedCacheResult.isFailure) {
            showOpenFileError(normalizedCacheResult.exceptionOrNull()!!)
            setContent(createEmptyContent())
            return@invokeLater
          }
          normalizedCache = normalizedCacheResult.getOrThrow().sorted()
          setContent(createNormalizedCacheContent())
          toolbar = createToolbar()
          setTabName(file.name)
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

  private class NormalizedCacheFieldTreeNode(val field: NormalizedCache.Field) : DefaultMutableTreeNode() {
    init {
      userObject = field.name
    }
  }
}
