package com.apollographql.ijplugin.normalizedcache.ui

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.normalizedcache.NormalizedCache
import com.intellij.ui.treeStructure.treetable.ListTreeTableModel
import com.intellij.ui.treeStructure.treetable.TreeTableModel
import com.intellij.util.ui.ColumnInfo
import javax.swing.tree.DefaultMutableTreeNode

class FieldTreeTableModel : ListTreeTableModel(
    DefaultMutableTreeNode(),
    arrayOf(
        object : ColumnInfo<Unit, Unit>(ApolloBundle.message("normalizedCacheViewer.fields.column.key")) {
          override fun getColumnClass() = TreeTableModel::class.java
          override fun valueOf(item: Unit) = Unit
        },
        object :
          ColumnInfo<NormalizedCacheFieldTreeNode, NormalizedCache.Field>(ApolloBundle.message("normalizedCacheViewer.fields.column.value")) {
          override fun getColumnClass() = NormalizedCache.Field::class.java
          override fun valueOf(item: NormalizedCacheFieldTreeNode) = item.field
        },
    ),
) {
  fun setRecord(record: NormalizedCache.Record) {
    setRoot(getRootNodeForRecord(record))
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

  class NormalizedCacheFieldTreeNode(val field: NormalizedCache.Field) : DefaultMutableTreeNode() {
    init {
      userObject = field.key
    }
  }
}
