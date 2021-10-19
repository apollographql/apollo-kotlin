package com.apollographql.apollo3.compiler.codegen

import com.squareup.moshi.JsonClass



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
  /**
   * CustomScalarTarget is a special case as there is no real generated reference pointing to it.
   * Instead it is a String in [CustomScalarType.className]
   */
  CustomScalarTarget,
  TestBuilder
}

@JsonClass(generateAdapter = true)
class ResolverEntry(
    val key: ResolverKey,
    val className: ResolverClassName
)
@JsonClass(generateAdapter = true)
class ResolverInfo(
    val magic: String,
    val version: String,
    val entries: List<ResolverEntry>
)