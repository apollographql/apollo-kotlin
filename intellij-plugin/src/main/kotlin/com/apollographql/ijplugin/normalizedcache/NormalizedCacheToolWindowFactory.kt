package com.apollographql.ijplugin.normalizedcache

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.ScrollingUtil
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBTreeTable
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.treeStructure.treetable.ListTreeTableModel
import com.intellij.ui.treeStructure.treetable.TreeTableModel
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListUiUtil
import java.awt.Cursor
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.ListSelectionModel
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.tree.DefaultMutableTreeNode

class NormalizedCacheToolWindowFactory : ToolWindowFactory, DumbAware, Disposable {
  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    return toolWindow.contentManager.addContent(
        ContentFactory.getInstance().createContent(
            NormalizedCacheValuesPanel(NormalizedCache.getFakeNormalizedCache()),
            "",
            false
        )
    )
  }

  override fun dispose() {}
}

class NormalizedCacheValuesPanel(
    private val normalizedCache: NormalizedCache,
) : SimpleToolWindowPanel(false, true) {
  private val fieldTreeTableModel = ListTreeTableModel(
      DefaultMutableTreeNode(),
      arrayOf(
          object : ColumnInfo<Unit, Unit>("Key") { //TODO
            override fun valueOf(item: Unit) = Unit
            override fun getColumnClass() = TreeTableModel::class.java
          },
          object : ColumnInfo<NormalizedCacheFieldTreeNode, NormalizedCache.Field>("Value") { // TODO
            override fun valueOf(item: NormalizedCacheFieldTreeNode) = item.field
            override fun getColumnClass() = NormalizedCache.Field::class.java
          },
      ),
  )

  private lateinit var keyList: JBList<NormalizedCache.Record>

  init {
    // TODO
    // setEmptyState("This is empty!")

    setContent(createSplitter())
  }

  // TODO use DSL?
  private fun createSplitter(): JComponent {
    val splitter = OnePixelSplitter(false, .25F).apply {
      firstComponent = createKeyList()
      secondComponent = createFieldTree()
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
  private fun createFieldTree(): JComponent {
    val tree = JBTreeTable(fieldTreeTableModel).apply {
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

      val mouseAdapter = object : MouseAdapter() {
        private fun getFieldAtPoint(e: MouseEvent): NormalizedCache.Field? {
          val point = Point(e.x, e.y)
          val row = table.rowAtPoint(point)
          // Add 1 to account for the tree column
          val column = table.columnAtPoint(point) + 1
          return table.model.getValueAt(row, column) as NormalizedCache.Field?
        }

        override fun mouseClicked(e: MouseEvent) {
          val field = getFieldAtPoint(e) ?: return
          if (field.value is NormalizedCache.FieldValue.Reference) {
            table.cursor = Cursor(Cursor.DEFAULT_CURSOR)
            val index = normalizedCache.records.indexOfFirst { it.key == field.value.key }
            if (index == -1) return
            keyList.selectedIndex = index
            keyList.ensureIndexIsVisible(keyList.selectedIndex)
          }
        }

        override fun mouseMoved(e: MouseEvent) {
          table.cursor = Cursor(
              if (getFieldAtPoint(e)?.value is NormalizedCache.FieldValue.Reference) {
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
    }
    return tree
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
