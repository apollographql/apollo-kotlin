package com.apollographql.apollo.compiler.codegen

import kotlinx.serialization.Serializable

@Serializable
class ResolverClassName(val packageName: String, val simpleNames: List<String>) {
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
