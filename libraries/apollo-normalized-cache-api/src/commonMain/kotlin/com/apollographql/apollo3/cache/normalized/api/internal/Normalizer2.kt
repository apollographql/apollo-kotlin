package com.apollographql.apollo3.cache.normalized.api.internal

import com.apollographql.apollo3.api.CompiledArgument
import com.apollographql.apollo3.api.CompiledField
import com.apollographql.apollo3.api.Executable
import com.apollographql.apollo3.api.CompiledSchema
import com.apollographql.apollo3.api.CompiledVariable
import com.apollographql.apollo3.api.ObjectType
import com.apollographql.apollo3.api.json.BufferedSinkJsonWriter
import com.apollographql.apollo3.api.json.writeAny
import com.apollographql.apollo3.api.resolveVariables
import com.apollographql.apollo3.ast.GQLBooleanValue
import com.apollographql.apollo3.ast.GQLEnumValue
import com.apollographql.apollo3.ast.GQLField
import com.apollographql.apollo3.ast.GQLFloatValue
import com.apollographql.apollo3.ast.GQLFragmentDefinition
import com.apollographql.apollo3.ast.GQLFragmentSpread
import com.apollographql.apollo3.ast.GQLInlineFragment
import com.apollographql.apollo3.ast.GQLIntValue
import com.apollographql.apollo3.ast.GQLListValue
import com.apollographql.apollo3.ast.GQLNullValue
import com.apollographql.apollo3.ast.GQLObjectValue
import com.apollographql.apollo3.ast.GQLOperationDefinition
import com.apollographql.apollo3.ast.GQLSelection
import com.apollographql.apollo3.ast.GQLStringValue
import com.apollographql.apollo3.ast.GQLValue
import com.apollographql.apollo3.ast.GQLVariableValue
import com.apollographql.apollo3.ast.parseAsGQLDocument
import com.apollographql.apollo3.ast.responseName
import com.apollographql.apollo3.ast.toUtf8
import com.apollographql.apollo3.cache.normalized.api.CacheKey
import com.apollographql.apollo3.cache.normalized.api.CacheKeyGenerator
import com.apollographql.apollo3.cache.normalized.api.CacheKeyGeneratorContext
import com.apollographql.apollo3.cache.normalized.api.Record
import okio.Buffer

/**
 * A [Normalizer2] takes a [Map]<String, Any?> and turns them into a flat list of [Record]
 * The key of each [Record] is given by [cacheKeyGenerator] or defaults to using the path
 */
internal class Normalizer2(
    private val variables: Executable.Variables,
    private val rootKey: String,
    private val cacheKeyGenerator: CacheKeyGenerator,
    graphqlDocument: String,
    private val compiledSchema: CompiledSchema,
) {
  private val fragmentDefinitions: Map<String, GQLFragmentDefinition>
  private val rootSelections: List<GQLSelection>

  init {
    val operationDefinitions = mutableListOf<GQLOperationDefinition>()
    val fragmentDefinitions = mutableListOf<GQLFragmentDefinition>()

    Buffer().writeUtf8(graphqlDocument)
        .parseAsGQLDocument()
        .getOrThrow()
        .definitions
        .forEach {
          when (it) {
            is GQLOperationDefinition -> operationDefinitions.add(it)
            is GQLFragmentDefinition -> fragmentDefinitions.add(it)
            else -> throw IllegalStateException("unexpected definition: ${it.toUtf8()}")
          }
        }

    rootSelections = if (operationDefinitions.isNotEmpty()) {
      operationDefinitions.single().selections
    } else {
      fragmentDefinitions.single().selections
    }

    this.fragmentDefinitions = fragmentDefinitions.associateBy { it.name }
  }

  private val records = mutableMapOf<String, Record>()

  fun normalize(map: Map<String, Any?>): Map<String, Record> {
    buildRecord(map, rootKey, rootSelections)

    return records
  }

  /**
   * @param obj the json node representing the object
   * @param key the key for this record
   * @param selections the selections queried on this object
   * @return the CacheKey
   */
  private fun buildRecord(
      obj: Map<String, Any?>,
      key: String,
      selections: List<GQLSelection>,
  ): CacheKey {

    val typename = obj["__typename"] as? String ?: throw IllegalStateException("No __typename")
    val allFields = collectFields(selections, typename)

    val record = Record(
        key = key,
        fields = obj.entries.mapNotNull { entry ->
          val GQLFields = allFields.filter { it.responseName() == entry.key }
          if (GQLFields.isEmpty()) {
            // If we come here, `obj` contains more data than the GQLSelections can understand
            // This happened previously (see https://github.com/apollographql/apollo-android/pull/3636)
            // It also happens if there's an always false @include directive (see https://github.com/apollographql/apollo-kotlin/issues/4772)
            // For all cache purposes, this is not part of the response and we therefore do not include this in the response
            return@mapNotNull null
          }
          val includedFields = GQLFields.filter {
            !it.shouldSkip(variableValues = variables.valueMap)
          }
          if (includedFields.isEmpty()) {
            // If the field is absent, we don't want to serialize "null" to the cache, do not include this field in the record.
            return@mapNotNull null
          }
          val mergedField = includedFields.first().copy(
              selections = includedFields.flatMap { it.selections }
          )

          val fieldKey = mergedField.toCompiledField().nameWithArguments(variables)

          val base = if (key == CacheKey.rootKey().key) {
            // If we're at the root level, skip `QUERY_ROOT` altogether to save a few bytes
            null
          } else {
            key
          }

          fieldKey to replaceObjects(
              entry.value,
              mergedField,
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
      field: GQLField,
      path: String,
  ): Any? {
    return when {
      value is List<*> -> {
        value.mapIndexed { index, item ->
          replaceObjects(item, field, path.append(index.toString()))
        }
      }

      value is Map<*, *> -> {
        @Suppress("UNCHECKED_CAST")
        val key = cacheKeyGenerator.cacheKeyForObject(
            value as Map<String, Any?>,
            CacheKeyGeneratorContext(field.toCompiledField(), variables),
        )?.key ?: path
        buildRecord(value, key, field.selections)
      }

      else -> {
        // scalar
        value
      }
    }
  }

  private class CollectState {
    val fields = mutableListOf<GQLField>()
  }

  private fun collectFields(selections: List<GQLSelection>, superTypes: Set<String>, state: CollectState) {
    selections.forEach {
      when (it) {
        is GQLField -> {
          state.fields.add(it)
        }

        is GQLInlineFragment -> {
          val typeCondition = it.typeCondition
          if (typeCondition == null || typeCondition.name in superTypes) {
            collectFields(it.selections, superTypes, state)
          }
        }

        is GQLFragmentSpread -> {
          val fragmentDefinition = fragmentDefinitions.get(it.name)
          if (fragmentDefinition != null && fragmentDefinition.typeCondition.name in superTypes) {
            collectFields(fragmentDefinition.selections, superTypes, state)
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
  private fun collectFields(selections: List<GQLSelection>, typename: String): List<GQLField> {
    val superTypes = compiledSchema.superTypes(typename)
    if (superTypes == null) {
      return emptyList()
    }
    val state = CollectState()
    collectFields(selections, superTypes, state)
    return state.fields
  }

  // The receiver can be null for the root query to save some space in the cache by not storing QUERY_ROOT all over the place
  private fun String?.append(next: String): String = if (this == null) next else "$this.$next"
}


private fun GQLValue.toAny(): Any? = when (this) {
  is GQLBooleanValue -> value
  is GQLEnumValue -> value
  is GQLFloatValue -> value
  is GQLIntValue -> value
  is GQLListValue -> values.map { it.toAny() }
  is GQLNullValue -> null
  is GQLObjectValue -> fields.map { it.name to it.value.toAny() }.toMap()
  is GQLStringValue -> value
  is GQLVariableValue -> CompiledVariable(name)
}

internal fun GQLField.toCompiledField(): CompiledField {
  return CompiledField.Builder(name, ObjectType.Builder("unknown").build())
      .arguments(
          arguments.map {
            CompiledArgument.Builder(it.name, it.value.toAny())
                .build()
          }
      )
      .build()

}