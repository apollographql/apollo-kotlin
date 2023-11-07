package com.apollographql.ijplugin.normalizedcache

data class NormalizedCache(
    val records: List<Record>,
) {
  data class Record(
      val key: String,
      val fields: List<Field>,
      val sizeInBytes: Int,
  )

  data class Field(
      val name: String,
      val value: FieldValue,
  )

  sealed interface FieldValue {
    data class StringValue(val value: String) : FieldValue
    data class NumberValue(val value: Number) : FieldValue
    data class BooleanValue(val value: Boolean) : FieldValue
    data class ListValue(val value: List<FieldValue>) : FieldValue

    /** For custom scalars. */
    data class CompositeValue(val value: List<Field>) : FieldValue
    data object Null : FieldValue
    data class Reference(val key: String) : FieldValue
  }

  fun sorted() = NormalizedCache(
      records.sortedWith(RecordKeyComparator)
  )

  companion object {
    val RecordKeyComparator = Comparator<Record> { o1, o2 ->
      when {
        o1.key == "QUERY_ROOT" -> -1
        o2.key == "QUERY_ROOT" -> 1
        else -> o1.key.compareTo(o2.key, ignoreCase = true)
      }
    }
  }
}
