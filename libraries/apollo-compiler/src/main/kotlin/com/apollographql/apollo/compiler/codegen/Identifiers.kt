package com.apollographql.apollo.compiler.codegen

internal object Identifier {
  const val __h: String = "__h"
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
  const val build = "build"
  const val fromJson = "fromJson"
  const val getPath = "getPath"
  const val serializeVariables = "serializeVariables"
  const val withDefaultValues = "withDefaultValues"
  const val toJson = "toJson"
  const val fields = "fields"
  const val rootField = "rootField"

  @Suppress("ObjectPropertyName")
  const val __typename = "__typename"
  const val typename = "typename"

  const val id = "id"
  const val name = "name"
  const val data = "data"
  const val root = "__root"
  const val selections = "selections"
  const val document = "document"
  const val equals = "equals"
  const val hashCode = "hashCode"
  const val toString = "toString"
  const val copy = "copy"
  const val Data = "Data"

  const val field = "field"
  const val __path = "__path"
  const val __fields = "__fields"

  const val OPERATION_DOCUMENT = "OPERATION_DOCUMENT"
  const val OPERATION_NAME = "OPERATION_NAME"
  const val OPERATION_ID = "OPERATION_ID"
  const val RESPONSE_NAMES = "RESPONSE_NAMES"

  const val Empty = "Empty"

  /**
   * Only used by generated code
   */
  const val safeValueOf = "safeValueOf"
  const val knownValues = "knownValues"
  const val knownEntries = "knownEntries"

  /**
   * UNKNOWN__ and KNOWN__ should probably have been __UNKNOWN because GraphQL reserves the leading __ but it's too late now.
   *
   * All in all it's not too bad because typing 'U', 'N', ... is usually more intuitive and in the very unlikely event that
   * there is a name clash, it can always be resolved with `@targetName`
   */
  const val UNKNOWN__ = "UNKNOWN__"
  const val KNOWN__ = "KNOWN__"
  const val rawValue = "rawValue"
  const val types = "types"
  const val block = "block"
  const val resolver = "resolver"
  const val newBuilder = "newBuilder"
  const val factory = "factory"
  const val Builder = "Builder"

  /**
   * Kotlin language identifiers
   */
  const val Companion = "Companion"

  /**
   * Java only
   */
  const val buildData = "buildData"
  const val map = "map"
}
