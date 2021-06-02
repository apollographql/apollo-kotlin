package com.apollographql.apollo3.api

sealed class CompiledSelection

class CompiledField(
    val name: String,
    val alias: String? = null,
    val type: CompiledType,
    val condition: List<CompiledCondition> = emptyList(),
    val arguments: Map<String, Any?> = emptyMap(),
    val selections: List<CompiledSelection> = emptyList(),
) : CompiledSelection()

class CompiledFragment(
    val typeCondition: String,
    val condition: List<CompiledCondition> = emptyList(),
    val selections: List<CompiledSelection> = emptyList(),
) : CompiledSelection()


class CompiledCondition(val name: String, val inverted: Boolean)

sealed class CompiledType

class CompiledNotNullType(val ofType: CompiledType) : CompiledType()
class CompiledListType(val ofType: CompiledType) : CompiledType()

/**
 * a named GraphQL type
 *
 * We make the distinction between objects and non-objects ones for the CacheKeyResolver API.
 * In a typical server scenario, the resolvers would have access to the schema and would look up the complete type
 * but we want to stay lightweight so for now we add this information
 */
sealed class CompiledNamedType(val name: String) : CompiledType()

/**
 * A GraphQL union, interface or object
 */
class CompiledCompoundType(name: String) : CompiledNamedType(name)

/**
 * Not compound: scalar or enum
 */
class CompiledOtherType(name: String) : CompiledNamedType(name)

fun CompiledType.notNull() = CompiledNotNullType(this)
fun CompiledType.list() = CompiledListType(this)

