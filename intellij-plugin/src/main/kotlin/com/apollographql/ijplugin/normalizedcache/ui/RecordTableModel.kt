package com.apollographql.ijplugin.normalizedcache.ui

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.normalizedcache.NormalizedCache
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel

class RecordTableModel(private val normalizedCache: NormalizedCache) : ListTableModel<NormalizedCache.Record>(
    object : ColumnInfo<NormalizedCache.Record, String>(ApolloBundle.message("normalizedCacheViewer.records.table.key")) {
      override fun valueOf(item: NormalizedCache.Record) = item.key
      override fun getComparator(): Comparator<NormalizedCache.Record> = NormalizedCache.RecordKeyComparator
    },
    object : ColumnInfo<NormalizedCache.Record, String>(ApolloBundle.message("normalizedCacheViewer.records.table.size")) {
      override fun valueOf(item: NormalizedCache.Record) = StringUtil.formatFileSize(item.sizeInBytes.toLong())
      override fun getComparator(): Comparator<NormalizedCache.Record> = Comparator.comparingInt { it.sizeInBytes }
    },
) {
  init {
    setItems(normalizedCache.records)
  }

  fun getRecordAt(row: Int): NormalizedCache.Record? {
    if (row < 0 || row >= rowCount) return null
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

  fun setFilter(filter: String) {
    setItems(normalizedCache.records.filter { it.key.contains(filter, ignoreCase = true) })
  }
}
