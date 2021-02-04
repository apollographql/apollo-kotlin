package com.apollographql.apollo.cache.normalized.internal

import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.cache.normalized.CacheKey
import com.apollographql.apollo.cache.normalized.CacheKeyResolver
import com.apollographql.apollo.cache.normalized.CacheReference
import com.apollographql.apollo.cache.normalized.Record

/**
 * Takes a AJObject and returns a list of [Record]
 */
internal class Normalizer(private val cacheKeyResolver: CacheKeyResolver) {

  private val records = mutableMapOf<String, Record>()

  fun normalize(obj: NormalizationIR.Element.Object, rootKey: String?): Map<String, Record> {
    obj.normalize(rootKey, null)
    return records
  }

  private fun NormalizationIR.Element.unwrap(): Any? = when (this) {
    is NormalizationIR.Element.Object -> fields.map { it.element.unwrap() }
    is NormalizationIR.Element.List -> elements.map { it.unwrap() }
    is NormalizationIR.Element.Scalar -> value
  }

  private fun NormalizationIR.Element.Object.normalize(path: String?, field: ResponseField?): CacheReference {
    val key = if (field == null) {
      path
    } else {
      val cacheKey = cacheKeyResolver.fromFieldRecordSet(
          field,
          fields.filter {
            /**
             * do not recurse in objects for now. This means users won't be able to use sub-fields to compute cache keys
             */
            it.element !is NormalizationIR.Element.Object
          }.map {
            it.fieldKey to it.element.unwrap()
          }.toMap()
      )

      if (cacheKey == CacheKey.NO_KEY) {
        path
      } else {
        cacheKey.key
      }
    }

    val actualKey = key ?: CacheKeyResolver.rootKey().key

    val fields = fields.map {
      it.fieldKey to when (val element = it.element) {
        is NormalizationIR.Element.Object -> element.normalize(key.append(it.fieldKey), it.field)
        is NormalizationIR.Element.Scalar -> element.value
        is NormalizationIR.Element.List -> element.normalize(key.append(it.fieldKey), it.field)
      }
    }.toMap()

    val record = Record(
        key = actualKey,
        fields = fields
    )

    val oldRecord = records[key]
    if (oldRecord != null) {
      records[key!!] = oldRecord.mergeWith(record).first
    } else {
      records[actualKey] = record
    }

    return CacheReference(actualKey)
  }

  // The receiver can be null for the root query to save some space in the cache by not storing QUERY_ROOT all over the place
  private fun String?.append(next: String) = if (this == null) next else "$this.$next"

  private fun NormalizationIR.Element.List.normalize(path: String, field: ResponseField): List<Any?> {
    return elements.mapIndexed { index, element ->
      when (element) {
        is NormalizationIR.Element.Scalar -> element.value
        is NormalizationIR.Element.List -> element.normalize(path.append(index.toString()), field)
        is NormalizationIR.Element.Object -> element.normalize(path.append(index.toString()), field)
      }
    }
  }
}

