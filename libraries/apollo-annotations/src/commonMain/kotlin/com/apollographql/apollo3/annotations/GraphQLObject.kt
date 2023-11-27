package com.apollographql.apollo3.annotations

/**
 * Marks a given class as a GraphQL object
 *
 * @param name the GraphQL name of the object. By default the name of the object
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
@MustBeDocumented
annotation class GraphQLObject(val name: String = "")
