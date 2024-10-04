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
      val key: String,
      val value: FieldValue,
  )

  sealed interface FieldValue {
    data class StringValue(val value: String) : FieldValue
    data class NumberValue(val value: String) : FieldValue
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
    /**
     * Compares records by key, with `QUERY_ROOT` first, then by alphabetical order, taking numbers into account.
     * E.g. `person.1` < `person.2` < `person.10` < `person.10.friend.1` < `person.10.friend.2`
     */
    val RecordKeyComparator = Comparator<Record> { o1, o2 ->
      val key1 = o1.key
      val key2 = o2.key
      when {
        key1 == "QUERY_ROOT" -> if (key2 == "QUERY_ROOT") 0 else -1
        key2 == "QUERY_ROOT" -> 1
        else -> {
          var idx1 = 0
          var idx2 = 0
          while (true) {
            val c1 = key1[idx1]
            val c2 = key2[idx2]
            if (c1.isDigit() && c2.isDigit()) {
              var number1 = 0
              var number2 = 0
              while (idx1 < key1.length && key1[idx1].isDigit()) {
                number1 = number1 * 10 + key1[idx1].digitToInt()
                idx1++
              }
              while (idx2 < key2.length && key2[idx2].isDigit()) {
                number2 = number2 * 10 + key2[idx2].digitToInt()
                idx2++
              }
              val comparison = number1.compareTo(number2)
              if (comparison != 0) {
                return@Comparator comparison
              }
            } else {
              val comparison = c1.lowercaseChar().compareTo(c2.lowercaseChar())
              if (comparison != 0) {
                return@Comparator comparison
              }
              idx1++
              idx2++
            }
            if (idx1 == key1.length || idx2 == key2.length) {
              break
            }
          }
          // If we're here, everything was equal up to the size of the smallest string. The smallest one should go first.
          key1.length.compareTo(key2.length)
        }
      }
    }
  }
}
