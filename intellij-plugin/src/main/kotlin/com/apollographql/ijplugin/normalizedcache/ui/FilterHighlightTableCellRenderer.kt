package com.apollographql.ijplugin.normalizedcache.ui

import com.intellij.openapi.util.TextRange
import com.intellij.ui.ColoredTableCellRenderer
import com.intellij.ui.speedSearch.SpeedSearchUtil
import javax.swing.JTable

class FilterHighlightTableCellRenderer : ColoredTableCellRenderer() {
  var filter: String? = null

  override fun customizeCellRenderer(
      table: JTable,
      value: Any?,
      selected: Boolean,
      hasFocus: Boolean,
      row: Int,
      column: Int,
  ) {
    append(value as String)

    filter?.let { filter ->
      if (filter.isEmpty()) return
      val matchIndex = value.indexOf(filter, ignoreCase = true)
      if (matchIndex == -1) return
      val textRange = TextRange(matchIndex, matchIndex + filter.length)
      SpeedSearchUtil.applySpeedSearchHighlighting(this, listOf(textRange), selected)
    }
  }
}
