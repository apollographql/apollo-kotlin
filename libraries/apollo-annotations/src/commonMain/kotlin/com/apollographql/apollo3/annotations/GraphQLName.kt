package com.apollographql.apollo3.annotations

/**
 * Changes the GraphQL name of a Kotlin symbol. By default:
 * - Kotlin properties are mapped to GraphQL fields/input fields of the same name
 * - Kotlin functions are mapped to GraphQL fields of the same name
 * - Kotlin classes are mapped to GraphQL object types/input object types of the same name
 * - etc...
 *
 * By using [GraphQLName], you can change this mapping
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION, AnnotationTarget.TYPEALIAS)
@MustBeDocumented
annotation class GraphQLName(val name: String)
