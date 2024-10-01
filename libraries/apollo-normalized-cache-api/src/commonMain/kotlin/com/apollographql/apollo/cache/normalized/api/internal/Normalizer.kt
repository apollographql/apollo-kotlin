package com.apollographql.apollo.cache.normalized.api.internal

import com.apollographql.apollo.api.CompiledField
import com.apollographql.apollo.api.CompiledFragment
import com.apollographql.apollo.api.CompiledListType
import com.apollographql.apollo.api.CompiledNamedType
import com.apollographql.apollo.api.CompiledNotNullType
import com.apollographql.apollo.api.CompiledSelection
import com.apollographql.apollo.api.CompiledType
import com.apollographql.apollo.api.Executable
import com.apollographql.apollo.api.isComposite
import com.apollographql.apollo.api.json.ApolloJsonElement
import com.apollographql.apollo.cache.normalized.api.CacheKey
import com.apollographql.apollo.cache.normalized.api.CacheKeyGenerator
import com.apollographql.apollo.cache.normalized.api.CacheKeyGeneratorContext
import com.apollographql.apollo.cache.normalized.api.Record

/**
 * A [Normalizer] takes a [Map]<String, Any?> and turns them into a flat list of [Record]
 * The key of each [Record] is given by [cacheKeyGenerator] or defaults to using the path
 */
internal class Normalizer(
    private val variables: Executable.Variables,
    private val rootKey: String,
    private val cacheKeyGenerator: CacheKeyGenerator,
) {
  private val records = mutableMapOf<String, Record>()

  fun normalize(map: Map<String, ApolloJsonElement>, selections: List<CompiledSelection>, parentType: String): Map<String, Record> {
    buildRecord(map, rootKey, selections, parentType)

    return records
  }

  /**
   * @param obj the json node representing the object
   * @param key the key for this record
   * @param selections the selections queried on this object
   * @return the CacheKey
   */
  private fun buildRecord(
      obj: Map<String, ApolloJsonElement>,
      key: String,
      selections: List<CompiledSelection>,
      parentType: String,
  ): CacheKey {

    val typename = obj["__typename"] as? String
    val allFields = collectFields(selections, parentType, typename)

    val record = Record(
        key = key,
        fields = obj.entries.mapNotNull { entry ->
          val compiledFields = allFields.filter { it.responseName == entry.key }
          if (compiledFields.isEmpty()) {
            // If we come here, `obj` contains more data than the CompiledSelections can understand
            // This happened previously (see https://github.com/apollographql/apollo-kotlin/pull/3636)
            // It also happens if there's an always false @include directive (see https://github.com/apollographql/apollo-kotlin/issues/4772)
            // For all cache purposes, this is not part of the response and we therefore do not include this in the response
            return@mapNotNull null
          }
          val includedFields = compiledFields.filter {
            !it.shouldSkip(variableValues = variables.valueMap)
          }
          if (includedFields.isEmpty()) {
            // If the field is absent, we don't want to serialize "null" to the cache, do not include this field in the record.
            return@mapNotNull null
          }
          val mergedField = includedFields.first().newBuilder()
              .selections(includedFields.flatMap { it.selections })
              .condition(emptyList())
              .build()

          val fieldKey = mergedField.nameWithArguments(variables)

          val base = if (key == CacheKey.rootKey().key) {
            // If we're at the root level, skip `QUERY_ROOT` altogether to save a few bytes
            null
          } else {
            key
          }

          fieldKey to replaceObjects(
              entry.value,
              mergedField,
              mergedField.type,
              base.append(fieldKey),
          )
        }.toMap()
    )

    val existingRecord = records[key]

    val mergedRecord = if (existingRecord != null) {
      /**
       * A query might contain the same object twice, we don't want to lose some fields when that happens
       */
      existingRecord.mergeWith(record).first
    } else {
      record
    }
    records[key] = mergedRecord

    return CacheKey(key)
  }


  /**
   * @param field the field currently being normalized
   * @param type_ the type currently being normalized. It can be different from [field.type] for lists.
   * Since the same field will be used for several objects in list, we can't map 1:1 anymore
   */
  private fun replaceObjects(
      value: Any?,
      field: CompiledField,
      type_: CompiledType,
      path: String,
  ): Any? {
    /**
     * Remove the NotNull decoration if needed
     */
    val type = if (type_ is CompiledNotNullType) {
      check(value != null)
      type_.ofType
    } else {
      if (value == null) {
        return null
      }
      type_
    }

    return when {
      type is CompiledListType -> {
        check(value is List<*>)
        value.mapIndexed { index, item ->
          replaceObjects(item, field, type.ofType, path.append(index.toString()))
        }
      }
      // Check for [isComposite] as we don't want to build a record for json scalars
      type is CompiledNamedType && type.isComposite() -> {
        check(value is Map<*, *>)
        @Suppress("UNCHECKED_CAST")
        val key = cacheKeyGenerator.cacheKeyForObject(
            value as Map<String, Any?>,
            CacheKeyGeneratorContext(field, variables),
        )?.key ?: path
        buildRecord(value, key, field.selections, field.type.rawType().name)
      }
      else -> {
        // scalar
        value
      }
    }
  }

  private class CollectState {
    val fields = mutableListOf<CompiledField>()
  }

  private fun collectFields(selections: List<CompiledSelection>, parentType: String, typename: String?, state: CollectState) {
    selections.forEach {
      when (it) {
        is CompiledField -> {
          state.fields.add(it)
        }
        is CompiledFragment -> {
          if (typename in it.possibleTypes || it.typeCondition == parentType) {
            collectFields(it.selections, parentType, typename, state)
          }
        }
      }
    }
  }

  /**
   * @param typename the typename of the object. It might be null if the `__typename` field wasn't queried. If
   * that's the case, we will collect less fields than we should and records will miss some values leading to more
   * cache miss
   */
  private fun collectFields(selections: List<CompiledSelection>, parentType: String, typename: String?): List<CompiledField> {
    val state = CollectState()
    collectFields(selections, parentType, typename, state)
    return state.fields
  }

  // The receiver can be null for the root query to save some space in the cache by not storing QUERY_ROOT all over the place
  private fun String?.append(next: String): String = if (this == null) next else "$this.$next"
}

