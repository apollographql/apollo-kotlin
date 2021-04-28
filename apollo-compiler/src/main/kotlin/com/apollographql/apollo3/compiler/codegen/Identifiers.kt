package com.apollographql.apollo3.compiler.codegen

internal object Identifier {
  const val responseAdapterCache = "responseAdapterCache"
  const val value = "value"
  const val reader = "reader"
  const val writer = "writer"
  const val toResponse = "toResponse"
  const val fromResponse = "fromResponse"
  const val serializeVariables = "serializeVariables"
  const val fields = "fields"
  const val fieldSets = "fieldSets"
  @Suppress("ObjectPropertyName")
  const val __typename = "__typename"
  const val typename = "typename"

  const val id = "id"
  const val name = "name"
  const val document = "document"

  const val OPERATION_DOCUMENT = "OPERATION_DOCUMENT"
  const val OPERATION_NAME = "OPERATION_NAME"
  const val OPERATION_ID = "OPERATION_ID"
  const val RESPONSE_NAMES = "RESPONSE_NAMES"
}