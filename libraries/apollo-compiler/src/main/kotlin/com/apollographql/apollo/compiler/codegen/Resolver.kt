package com.apollographql.apollo.compiler.codegen

import kotlinx.serialization.Serializable

@Serializable
class ResolverClassName(val packageName: String, val simpleNames: List<String>) {
  constructor(packageName: String, vararg simpleNames: String): this(packageName, simpleNames.toList())
}

/**
 * Must be a data class because it is used as a key in resolvers
 */
@Serializable
data class ResolverKey(val kind: ResolverKeyKind, val id: String)

enum class ResolverKeyKind {
  /**
   * `id` is the name of the GraphQL type
   */
  SchemaType,
  Model,
  SchemaTypeAdapter,
  ModelAdapter,
  Operation,
  OperationVariablesAdapter,
  OperationSelections,
  Fragment,
  FragmentVariablesAdapter,
  FragmentSelections,
  Schema,
  CustomScalarAdapters,
  Pagination,
  ArgumentDefinition,
  JavaOptionalAdapter,
  JavaOptionalAdapters,
}

@Serializable
class ResolverEntry(
    val key: ResolverKey,
    val className: ResolverClassName
)
