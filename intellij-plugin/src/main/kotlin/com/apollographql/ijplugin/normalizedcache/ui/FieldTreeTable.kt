package com.apollographql.ijplugin.normalizedcache.ui

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.normalizedcache.NormalizedCache
import com.intellij.ide.DefaultTreeExpander
import com.intellij.ide.TreeExpander
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.ui.JBMenuItem
import com.intellij.openapi.ui.JBPopupMenu
import com.intellij.ui.ColoredTableCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBTreeTable
import com.intellij.util.ui.UIUtil
import java.awt.Color
import java.awt.Component
import java.awt.Cursor
import java.awt.Point
import java.awt.datatransfer.StringSelection
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JTable
import javax.swing.SwingUtilities
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener
import javax.swing.tree.TreePath

@Suppress("UnstableApiUsage")
class FieldTreeTable(selectRecord: (String) -> Unit) : JBTreeTable(FieldTreeTableModel()) {
  val treeExpander: TreeExpander = DefaultTreeExpander { tree }

  init {
    columnProportion = .8F
    setDefaultRenderer(
        NormalizedCache.Field::class.java,
        object : ColoredTableCellRenderer() {
          override fun customizeCellRenderer(table: JTable, value: Any?, selected: Boolean, hasFocus: Boolean, row: Int, column: Int) {
            value as NormalizedCache.Field
            when (val v = value.value) {
              is NormalizedCache.FieldValue.StringValue -> append("\"${v.value}\"")
              is NormalizedCache.FieldValue.NumberValue -> append(v.value)
              is NormalizedCache.FieldValue.BooleanValue -> append(v.value.toString())
              is NormalizedCache.FieldValue.ListValue -> append(
                  when (val size = v.value.size) {
                    0 -> ApolloBundle.message("normalizedCacheViewer.fields.list.empty")
                    1 -> ApolloBundle.message("normalizedCacheViewer.fields.list.single")
                    else -> ApolloBundle.message("normalizedCacheViewer.fields.list.multiple", size)
                  },
                  SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES
              )

              is NormalizedCache.FieldValue.CompositeValue -> append("{...}", SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES)
              NormalizedCache.FieldValue.Null -> append("null")
              is NormalizedCache.FieldValue.Reference -> {
                append("→ ", SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES)
                append(v.key, SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES)
              }
            }
          }
        }
    )

    // Handle reference clicks and cursor changes
    val mouseAdapter = object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        if (e.button != MouseEvent.BUTTON1) return
        table.cursor = Cursor(Cursor.DEFAULT_CURSOR)
        val field = getFieldAtPoint(e) ?: return
        if (field.value is NormalizedCache.FieldValue.Reference) {
          selectRecord(field.value.key)
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

    table.isStriped = true

    installPopupMenu()
  }

  override fun getPathBackground(path: TreePath, row: Int): Color? {
    return if (row % 2 == 0) {
      UIUtil.getDecoratedRowColor()
    } else {
      null
    }
  }

  override fun getModel(): FieldTreeTableModel = super.getModel() as FieldTreeTableModel

  fun setRecord(record: NormalizedCache.Record) {
    model.setRecord(record)
  }

  private fun installPopupMenu() {
    val popupMenu = object : JBPopupMenu() {
      override fun show(invoker: Component, x: Int, y: Int) {
        when (getFieldAtPoint(x, y)?.value) {
          is NormalizedCache.FieldValue.StringValue,
          is NormalizedCache.FieldValue.NumberValue,
          is NormalizedCache.FieldValue.BooleanValue,
          is NormalizedCache.FieldValue.Reference,
          NormalizedCache.FieldValue.Null,
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
          is NormalizedCache.FieldValue.StringValue -> value.value
          is NormalizedCache.FieldValue.NumberValue -> value.value
          is NormalizedCache.FieldValue.BooleanValue -> value.value.toString()
          is NormalizedCache.FieldValue.Reference -> value.key
          NormalizedCache.FieldValue.Null -> "null"
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

  private fun getFieldAtPoint(x: Int, y: Int): NormalizedCache.Field? {
    val point = Point(x, y)
    val row = table.rowAtPoint(point)
    // Add 1 to account for the tree column
    val column = table.columnAtPoint(point) + 1
    return table.model.getValueAt(row, column) as NormalizedCache.Field?
  }

  private fun getFieldAtPoint(e: MouseEvent): NormalizedCache.Field? {
    return getFieldAtPoint(e.x, e.y)
  }
}
