package com.apollographql.apollo3.cache.normalized.api.internal

import com.apollographql.apollo3.api.CompiledField
import com.apollographql.apollo3.api.CompiledFragment
import com.apollographql.apollo3.api.CompiledListType
import com.apollographql.apollo3.api.CompiledNamedType
import com.apollographql.apollo3.api.CompiledNotNullType
import com.apollographql.apollo3.api.CompiledSelection
import com.apollographql.apollo3.api.CompiledType
import com.apollographql.apollo3.api.Executable
import com.apollographql.apollo3.api.InterfaceType
import com.apollographql.apollo3.api.ObjectType
import com.apollographql.apollo3.api.isComposite
import com.apollographql.apollo3.cache.normalized.api.CacheKey
import com.apollographql.apollo3.cache.normalized.api.CacheKeyGenerator
import com.apollographql.apollo3.cache.normalized.api.CacheKeyGeneratorContext
import com.apollographql.apollo3.cache.normalized.api.MetadataGenerator
import com.apollographql.apollo3.cache.normalized.api.MetadataGeneratorContext
import com.apollographql.apollo3.cache.normalized.api.Record

/**
 * A [Normalizer] takes a [Map]<String, Any?> and turns them into a flat list of [Record]
 * The key of each [Record] is given by [cacheKeyGenerator] or defaults to using the path
 */
internal class Normalizer(
    private val variables: Executable.Variables,
    private val rootKey: String,
    private val cacheKeyGenerator: CacheKeyGenerator,
    private val metadataGenerator: MetadataGenerator,
) {
  private val records = mutableMapOf<String, Record>()

  fun normalize(map: Map<String, Any?>, selections: List<CompiledSelection>, typeInScope: CompiledNamedType): Map<String, Record> {
    buildRecord(map, rootKey, selections, typeInScope.name, typeInScope.embeddedFields)

    return records
  }

  private class FieldInfo(
      val fieldValue: Any?,
      val arguments: Map<String, Any?>,
      val metadata: Map<String, Any?>,
  )

  /**
   *
   *
   * @param obj the json node representing the object
   * @param key the key for this record
   * @param selections the selections queried on this object
   * @return the CacheKey if this object has a CacheKey or the new Map if the object was embedded
   */
  private fun buildFields(
      obj: Map<String, Any?>,
      key: String,
      selections: List<CompiledSelection>,
      typeInScope: String,
      embeddedFields: List<String>,
  ): Map<String, FieldInfo> {

    val typename = obj["__typename"] as? String
    val allFields = collectFields(selections, typeInScope, typename)

    val fields = obj.entries.mapNotNull { entry ->
      val compiledFields = allFields.filter { it.responseName == entry.key }
      if (compiledFields.isEmpty()) {
        // If we come here, `obj` contains more data than the CompiledSelections can understand
        // This happened previously (see https://github.com/apollographql/apollo-android/pull/3636)
        // This should not happen anymore but in case it does, we're logging some info here
        throw RuntimeException("Cannot find a CompiledField for entry: {${entry.key}: ${entry.value}}, __typename = $typename, key = $key")
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
      val value = replaceObjects(
          entry.value,
          mergedField,
          mergedField.type,
          base.append(fieldKey),
          embeddedFields
      )
      val arguments = mergedField.argumentsWithValue(variables)
      val metadata = metadataGenerator.metadataForObject(entry.value, MetadataGeneratorContext(field = mergedField, variables))
      fieldKey to FieldInfo(value, arguments, metadata)
    }.toMap()

    return fields
  }

  /**
   *
   *
   * @param obj the json node representing the object
   * @param key the key for this record
   * @param selections the selections queried on this object
   * @return the CacheKey if this object has a CacheKey or the new Map if the object was embedded
   */
  private fun buildRecord(
      obj: Map<String, Any?>,
      key: String,
      selections: List<CompiledSelection>,
      typeInScope: String,
      embeddedFields: List<String>,
  ): CacheKey {
    val fields = buildFields(obj, key, selections, typeInScope, embeddedFields)
    val fieldValues = fields.mapValues { it.value.fieldValue }
    val arguments = fields.mapValues { it.value.arguments }
    val metadata = fields.mapValues { it.value.metadata }
    val record = Record(
        key = key,
        fields = fieldValues,
        mutationId = null,
        date = emptyMap(),
        arguments = arguments,
        metadata = metadata,
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
   * Replace all objects in [value] with [CacheKey] and if [value] is an object itself, returns it as a [CacheKey]
   *
   * This function builds the list of records as a side effect
   *
   * @param value a json value from the response. Can be any type supported by [com.apollographql.apollo3.api.json.JsonWriter]
   * @param field the field currently being normalized
   * @param type_ the type currently being normalized. It can be different from [field.type] for lists.
   * @param embeddedFields the embedded fields of the parent
   */
  private fun replaceObjects(
      value: Any?,
      field: CompiledField,
      type_: CompiledType,
      path: String,
      embeddedFields: List<String>,
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
          replaceObjects(item, field, type.ofType, path.append(index.toString()), embeddedFields)
        }
      }
      // Check for [isComposite] as we don't want to build a record for json scalars
      type is CompiledNamedType && type.isComposite() -> {
        check(value is Map<*, *>)
        @Suppress("UNCHECKED_CAST")
        var key = cacheKeyGenerator.cacheKeyForObject(
            value as Map<String, Any?>,
            CacheKeyGeneratorContext(field, variables),
        )?.key

        if (key == null) {
          key = path
        }
        if (embeddedFields.contains(field.name)) {
          buildFields(value, key, field.selections, field.type.leafType().name, field.type.leafType().embeddedFields)
              .mapValues { it.value.fieldValue }
        } else {
          buildRecord(value, key, field.selections, field.type.leafType().name, field.type.leafType().embeddedFields)
        }
      }
      else -> {
        // scalar
        value
      }
    }
  }

  private val CompiledNamedType.embeddedFields: List<String>
    get() = when (this) {
      is ObjectType -> embeddedFields
      is InterfaceType -> embeddedFields
      else -> emptyList()
    }

  private class CollectState {
    val fields = mutableListOf<CompiledField>()
  }

  private fun collectFields(selections: List<CompiledSelection>, typeInScope: String, typename: String?, state: CollectState) {
    selections.forEach {
      when (it) {
        is CompiledField -> {
          state.fields.add(it)
        }
        is CompiledFragment -> {
          if (typename in it.possibleTypes || it.typeCondition == typeInScope) {
            collectFields(it.selections, typeInScope, typename, state)
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
  private fun collectFields(selections: List<CompiledSelection>, typeInScope: String, typename: String?): List<CompiledField> {
    val state = CollectState()
    collectFields(selections, typeInScope, typename, state)
    return state.fields
  }

  // The receiver can be null for the root query to save some space in the cache by not storing QUERY_ROOT all over the place
  private fun String?.append(next: String): String = if (this == null) next else "$this.$next"

  private fun CompiledField.argumentsWithValue(variables: Executable.Variables): Map<String, Any?> {
    return arguments.associate { it.name to resolveArgument(it.name, variables) }
  }
}
