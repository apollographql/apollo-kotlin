package com.apollographql.apollo3.compiler.codegen

import kotlinx.serialization.Serializable

/**
 * Additional resolver data generated alongside the models and adapters.
 * This data maps a GraphQL identifier (such as a typename or model path) to its target class.
 * This allows children modules to reference classes generated in parents (and know when to skip generating them).
 */
@Serializable
internal class ResolverInfo(
    val magic: String,
    val version: String,
    val entries: List<ResolverEntry>
)

@Serializable
internal class ResolverClassName(val packageName: String, val simpleNames: List<String>) {
  constructor(packageName: String, vararg simpleNames: String): this(packageName, simpleNames.toList())
}

@Serializable
internal class ResolverMemberName(val className: ResolverClassName, val name: String)

/**
 * Must be a data class because it is used as a key in resolvers
 */
@Serializable
data class ResolverKey(val kind: ResolverKeyKind, val id: String)

enum class ResolverKeyKind {
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
  MapType,
  BuilderType,
  BuilderFun,
  Schema,
  CustomScalarAdapters,
  Pagination
}

@Serializable
internal class ResolverEntry(
    val key: ResolverKey,
    val className: ResolverClassName
)
