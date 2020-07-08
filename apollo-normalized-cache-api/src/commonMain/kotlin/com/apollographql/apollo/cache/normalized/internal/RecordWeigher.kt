package com.apollographql.apollo.cache.normalized.internal

import com.apollographql.apollo.api.BigDecimal
import com.apollographql.apollo.cache.normalized.CacheReference
import com.apollographql.apollo.cache.normalized.Record
import okio.internal.commonAsUtf8ToByteArray
import kotlin.jvm.JvmStatic

object RecordWeigher {
  private const val SIZE_OF_BOOLEAN = 16
  private const val SIZE_OF_BIG_DECIMAL = 32
  private const val SIZE_OF_ARRAY_OVERHEAD = 16
  private const val SIZE_OF_RECORD_OVERHEAD = 16
  private const val SIZE_OF_CACHE_REFERENCE_OVERHEAD = 16
  private const val SIZE_OF_NULL = 4

  @JvmStatic
  fun byteChange(newValue: Any?, oldValue: Any?): Int {
    return weighField(newValue) - weighField(oldValue)
  }

  @JvmStatic
  fun calculateBytes(record: Record): Int {
    var size = SIZE_OF_RECORD_OVERHEAD + record.key.commonAsUtf8ToByteArray().size
    for ((key, value) in record.fields) {
      size += key.commonAsUtf8ToByteArray().size + weighField(value)
    }
    return size
  }

  private fun weighField(field: Any?): Int {
    return when (field) {
      null -> SIZE_OF_NULL
      is String -> field.commonAsUtf8ToByteArray().size
      is Boolean -> SIZE_OF_BOOLEAN
      is BigDecimal -> SIZE_OF_BIG_DECIMAL
      is List<*> -> {
        SIZE_OF_ARRAY_OVERHEAD + field.sumBy { weighField(it) }
      }
      is CacheReference -> {
        SIZE_OF_CACHE_REFERENCE_OVERHEAD + field.key.commonAsUtf8ToByteArray().size
      }
      else -> error("Unknown field type in Record. ${field::class.qualifiedName}")
    }
  }
}
