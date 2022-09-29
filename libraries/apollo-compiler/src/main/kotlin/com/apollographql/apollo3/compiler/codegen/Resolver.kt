package com.apollographql.apollo3.compiler.codegen

import com.squareup.moshi.JsonClass

/**
 * Additional resolver data generated alongside the models and adapters.
 * This data maps a GraphQL identifier (such as a typename or model path) to its target class.
 * This allows children modules to reference classes generated in parents (and know when to skip generating them).
 */
@JsonClass(generateAdapter = true)
class ResolverInfo(
    val magic: String,
    val version: String,
    val entries: List<ResolverEntry>
)

@JsonClass(generateAdapter = true)
class ResolverClassName(val packageName: String, val simpleNames: List<String>) {
  constructor(packageName: String, vararg simpleNames: String): this(packageName, simpleNames.toList())
}

@JsonClass(generateAdapter = true)
class ResolverMemberName(val className: ResolverClassName, val name: String)

/**
 * Must be a data class because it is used as a key in resolvers
 */
@JsonClass(generateAdapter = true)
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
  TestBuilder,
  MapType,
  BuilderType,
  BuilderFun,
  Schema,
  CustomScalarAdapters,
  Pagination
}

@JsonClass(generateAdapter = true)
class ResolverEntry(
    val key: ResolverKey,
    val className: ResolverClassName
)
