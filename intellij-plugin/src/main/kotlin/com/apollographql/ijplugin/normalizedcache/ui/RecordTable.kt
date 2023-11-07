package com.apollographql.ijplugin.normalizedcache.ui

import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.NamedColorUtil
import java.awt.Component
import java.awt.event.KeyEvent
import javax.swing.JTable
import javax.swing.SwingConstants
import javax.swing.table.DefaultTableCellRenderer

class RecordTable(recordTableModel: RecordTableModel) : JBTable(recordTableModel) {
  private val filterHighlightTableCellRenderer = FilterHighlightTableCellRenderer()

  init {
    columnModel.getColumn(0).cellRenderer = filterHighlightTableCellRenderer
    columnModel.getColumn(1).cellRenderer = object : DefaultTableCellRenderer() {
      override fun getTableCellRendererComponent(
          table: JTable?,
          value: Any?,
          isSelected: Boolean,
          hasFocus: Boolean,
          row: Int,
          column: Int,
      ): Component {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
        font = JBFont.small()
        return this
      }
    }.apply {
      horizontalAlignment = SwingConstants.RIGHT
      foreground = NamedColorUtil.getInactiveTextColor()
    }

    columnModel.getColumn(1).maxWidth = 50
  }

  fun setFilter(filter: () -> String) {
    filterHighlightTableCellRenderer.filter = filter
  }

  public override fun processKeyEvent(e: KeyEvent) {
    super.processKeyEvent(e)
  }

  override fun getModel(): RecordTableModel = super.getModel() as RecordTableModel

  fun selectRecord(key: String) {
    val index = model.indexOfRecord(key)
    if (index != -1) {
      selectionModel.setSelectionInterval(index, index)
      scrollRectToVisible(getCellRect(selectedRow, 0, true).apply {
        y -= height / 2
        height *= 2
      })
    }
  }
}
