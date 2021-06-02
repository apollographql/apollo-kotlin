package com.apollographql.apollo3.api

sealed class CompiledSelection

data class CompiledField(
    val alias: String?,
    val name: String,
    val condition: List<CompiledCondition>,
    val arguments: Map<String, Any?> = emptyMap(),
    val selections: List<CompiledSelection>,
) : CompiledSelection()

data class CompiledInlineFragment(
    val typeCondition: String,
    val condition: List<CompiledCondition>,
    val selections: List<CompiledSelection>,
) : CompiledSelection()

data class CompiledFragmentSpread(
    val condition: List<CompiledCondition>,
    val definition: CompiledFragmentDefinition,
) : CompiledSelection()

data class CompiledFragmentDefinition(
    val typeCondition: String,
    val selections: List<CompiledSelection>,
)

class CompiledCondition(val name: String, val inverted: Boolean)

sealed class CompiledType

data class CompiledNotNullType(val ofType: CompiledType) : CompiledType()
data class CompiledListType(val ofType: CompiledType) : CompiledType()

/**
 * a named GraphQL type
 *
 * We make the distinction between objects and non-objects ones for the CacheKeyResolver API.
 * In a typical server scenario, the resolvers would have access to the schema and would look up the complete type
 * but we want to stay lightweight so for now we add this information
 */
sealed class CompiledNamedType(val name: String) : CompiledType()

/**
 * This is field is a Kotlin object. It can be a GraphQL union, interface or object
 */
class CompiledCompoundType(name: String) : CompiledNamedType(name)

/**
 * Not compound: scalar or enum
 */
class CompiledOtherType(name: String) : CompiledNamedType(name)
