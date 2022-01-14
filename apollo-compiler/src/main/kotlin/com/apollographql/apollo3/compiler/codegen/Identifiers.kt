package com.apollographql.apollo3.compiler.codegen

internal object Identifier {
  const val adapter = "adapter"
  const val evaluate = "evaluate"
  const val notNull = "notNull"

  const val list = "list"
  const val optional = "optional"
  const val nullable = "nullable"

  const val type = "type"
  const val customScalarAdapters = "customScalarAdapters"
  const val value = "value"
  const val reader = "reader"
  const val writer = "writer"
  const val toJson = "toJson"
  const val fromJson = "fromJson"
  const val serializeVariables = "serializeVariables"
  const val fields = "fields"
  const val rootField = "rootField"
  @Suppress("ObjectPropertyName")
  const val __typename = "__typename"
  const val typename = "typename"

  const val id = "id"
  const val name = "name"
  const val data = "data"
  const val root = "root"
  const val selections = "selections"
  const val document = "document"
  const val Data = "Data"

  const val cacheKeyForObject = "cacheKeyForObject"
  const val field = "field"
  const val __map = "__map"


  const val OPERATION_DOCUMENT = "OPERATION_DOCUMENT"
  const val OPERATION_NAME = "OPERATION_NAME"
  const val OPERATION_ID = "OPERATION_ID"
  const val RESPONSE_NAMES = "RESPONSE_NAMES"

  /**
   * Only used by generated code
   */
  const val safeValueOf = "safeValueOf"
  const val knownValues = "knownValues"
  // extra underscores at the end to prevent potential name clashes
  const val UNKNOWN__ = "UNKNOWN__"
  const val rawValue = "rawValue"
  const val types = "types"
  const val testResolver = "testResolver"
  const val block = "block"

  /**
   * Kotlin language identifiers
   */
  const val Companion = "Companion"
}
