package com.apollographql.ijplugin.normalizedcache

import com.apollographql.ijplugin.ApolloBundle
import com.intellij.icons.AllIcons
import com.intellij.ide.CommonActionsManager
import com.intellij.ide.DefaultTreeExpander
import com.intellij.ide.TreeExpander
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
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
import com.intellij.ui.ScrollingUtil
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBPanelWithEmptyText
import com.intellij.ui.components.JBTreeTable
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManager
import com.intellij.ui.treeStructure.treetable.ListTreeTableModel
import com.intellij.ui.treeStructure.treetable.TreeTableModel
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.ListUiUtil
import java.awt.Cursor
import java.awt.Point
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.ListSelectionModel
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.tree.DefaultMutableTreeNode

class NormalizedCacheToolWindowFactory : ToolWindowFactory, DumbAware, Disposable {
  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    createNewTab(toolWindow.contentManager)

    val newTabAction = object : DumbAwareAction(ApolloBundle.messagePointer("normalizedCacheViewer.newTab"), AllIcons.General.Add) {
      override fun actionPerformed(e: AnActionEvent) {
        createNewTab(toolWindow.contentManager)
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
              createNewTab(toolWindow.contentManager)
            }
          }
        }
    )
  }

  private fun createNewTab(contentManager: ContentManager) {
    val setTabName: (String) -> Unit = {
      contentManager.selectedContent?.displayName = it
    }
    contentManager.addContent(
        ContentFactory.getInstance().createContent(
            NormalizedCacheWindowPanel(setTabName),
            "Empty", // TODO
            false
        )
    )
    contentManager.setSelectedContent(contentManager.contents.last())
  }

  override fun dispose() {}
}

class NormalizedCacheWindowPanel(private val setTabName: (String) -> Unit) : SimpleToolWindowPanel(false, true) {
  private lateinit var normalizedCache: NormalizedCache

  private val fieldTreeTableModel = ListTreeTableModel(
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

  private lateinit var fieldTreeExpander: TreeExpander
  private lateinit var keyList: JBList<NormalizedCache.Record>

  init {
    setContent(createEmptyContent())
  }

  private fun createEmptyContent(): JComponent {
    return JBPanelWithEmptyText().apply {
      emptyText.text = ApolloBundle.message("normalizedCacheViewer.empty.message")
      emptyText.appendLine(ApolloBundle.message("normalizedCacheViewer.empty.openFile"), SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES) {
        openFile()
      }
    }
  }

  private fun openFile() {
    // TODO
    normalizedCache = NormalizedCache.getFakeNormalizedCache()
    setContent(createNormalizedCacheContent())
    toolbar = createToolbar()
    setTabName("xxx-aaa-bbb.db")
  }

  private fun createToolbar(): JComponent {
    val group = DefaultActionGroup()
    group.add(CommonActionsManager.getInstance().createExpandAllAction(fieldTreeExpander, this).apply {
      getTemplatePresentation().setDescription(ApolloBundle.message("normalizedCacheViewer.toolbar.expandAll"))
    })
    group.add(CommonActionsManager.getInstance().createCollapseAllAction(fieldTreeExpander, this).apply {
      getTemplatePresentation().setDescription(ApolloBundle.message("normalizedCacheViewer.toolbar.collapseAll"))
    })

    val actionToolBar = ActionManager.getInstance().createActionToolbar(NormalizedCacheWindowPanel::class.java.name, group, false);
    actionToolBar.targetComponent = this
    return JBUI.Panels.simplePanel(actionToolBar.component)
  }

  private fun createNormalizedCacheContent(): JComponent {
    val splitter = OnePixelSplitter(false, .25F).apply {
      firstComponent = createKeyList()
      secondComponent = createFieldTreeTable()
      setResizeEnabled(true)
      splitterProportionKey = "${NormalizedCacheToolWindowFactory::class.java}.splitterProportionKey"
    }
    return splitter
  }

  private fun createKeyList(): JComponent {
    val listModel = DefaultListModel<NormalizedCache.Record>().apply {
      addAll(normalizedCache.records)
    }
    keyList = JBList(listModel).apply {
      selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
      ScrollingUtil.installActions(this)
      ListUiUtil.Selection.installSelectionOnFocus(this)
      ListUiUtil.Selection.installSelectionOnRightClick(this)

      cellRenderer = SimpleListCellRenderer.create("") { it.key }

      addListSelectionListener {
        fieldTreeTableModel.setRoot(getRootNodeForRecord(selectedValue))
      }

      selectedIndex = 0
    }
    return ScrollPaneFactory.createScrollPane(keyList)
  }

  @Suppress("UnstableApiUsage")
  private fun createFieldTreeTable(): JComponent {
    val treeTable = JBTreeTable(fieldTreeTableModel).apply {
      columnProportion = .75F
      setDefaultRenderer(NormalizedCache.Field::class.java, object : DefaultTableCellRenderer() {
        override fun setValue(value: Any?) {
          value as NormalizedCache.Field
          val formatted = when (val v = value.value) {
            is NormalizedCache.FieldValue.StringValue -> "\"${v.value}\""
            is NormalizedCache.FieldValue.NumberValue -> v.value.toString()
            is NormalizedCache.FieldValue.BooleanValue -> v.value.toString()
            is NormalizedCache.FieldValue.ListValue -> "<html><i>[${v.value.size} items]" // TODO
            is NormalizedCache.FieldValue.CompositeValue -> "<html><i>{...}"
            NormalizedCache.FieldValue.Null -> "null"
            is NormalizedCache.FieldValue.Reference -> "<html>→ <u><a href=\"\">${v.key}"
          }
          text = formatted
        }
      })

      // Handle reference clicks and cursor changes
      val mouseAdapter = object : MouseAdapter() {
        private fun getFieldUnderPointer(e: MouseEvent): NormalizedCache.Field? {
          val point = Point(e.x, e.y)
          val row = table.rowAtPoint(point)
          // Add 1 to account for the tree column
          val column = table.columnAtPoint(point) + 1
          return table.model.getValueAt(row, column) as NormalizedCache.Field?
        }

        override fun mouseClicked(e: MouseEvent) {
          table.cursor = Cursor(Cursor.DEFAULT_CURSOR)
          val field = getFieldUnderPointer(e) ?: return
          if (field.value is NormalizedCache.FieldValue.Reference) {
            val index = normalizedCache.records.indexOfFirst { it.key == field.value.key }
            if (index == -1) return
            keyList.selectedIndex = index
            keyList.ensureIndexIsVisible(keyList.selectedIndex)
          }
        }

        override fun mouseMoved(e: MouseEvent) {
          table.cursor = Cursor(
              if (getFieldUnderPointer(e)?.value is NormalizedCache.FieldValue.Reference) {
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

      fieldTreeExpander = DefaultTreeExpander { tree }
    }
    return treeTable
  }

  private fun getRootNodeForRecord(record: NormalizedCache.Record) = DefaultMutableTreeNode().apply {
    addFields(record.fields)
  }

  private fun DefaultMutableTreeNode.addFields(fields: List<NormalizedCache.Field>) {
    for (field in fields) {
      val childNode = NormalizedCacheFieldTreeNode(field)
      add(childNode)
      when (val value = field.value) {
        is NormalizedCache.FieldValue.ListValue -> childNode.addFields(value.value.mapIndexed { i, v -> NormalizedCache.Field(i.toString(), v) })
        is NormalizedCache.FieldValue.CompositeValue -> childNode.addFields(value.value)
        else -> {}
      }
    }
  }
}

private class NormalizedCacheFieldTreeNode(val field: NormalizedCache.Field) : DefaultMutableTreeNode() {
  init {
    userObject = field.name
  }
}
