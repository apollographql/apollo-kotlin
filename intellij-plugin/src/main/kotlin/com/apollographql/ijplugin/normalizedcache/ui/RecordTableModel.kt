package com.apollographql.ijplugin.normalizedcache.ui

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.normalizedcache.NormalizedCache
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.speedSearch.FilteringTableModel
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel

private class CoreRecordTableModel(normalizedCache: NormalizedCache) : ListTableModel<NormalizedCache.Record>(
    object : ColumnInfo<NormalizedCache.Record, String>(ApolloBundle.message("normalizedCacheViewer.records.table.key")) {
      override fun valueOf(item: NormalizedCache.Record) = item.key
    },
    object : ColumnInfo<NormalizedCache.Record, String>(ApolloBundle.message("normalizedCacheViewer.records.table.size")) {
      override fun valueOf(item: NormalizedCache.Record) = StringUtil.formatFileSize(item.sizeInBytes.toLong())
    },
) {
  init {
    setItems(normalizedCache.records)
  }
}

class RecordTableModel(private val normalizedCache: NormalizedCache) : FilteringTableModel<String>(CoreRecordTableModel(normalizedCache), String::class.java) {
  init {
    refilter()
  }

  fun getRecordAt(row: Int): NormalizedCache.Record? {
    return getValueAt(row, 0)?.let { selectedKey ->
      normalizedCache.records.first { it.key == selectedKey }
    }
  }

  fun isRecordShowing(key: String): Boolean {
    return indexOfRecord(key) != -1
  }

  fun indexOfRecord(key: String): Int {
    for (i in 0 until rowCount) {
      val value = getValueAt(i, 0)
      if (value == key) {
        return i
      }
    }
    return -1
  }
}
