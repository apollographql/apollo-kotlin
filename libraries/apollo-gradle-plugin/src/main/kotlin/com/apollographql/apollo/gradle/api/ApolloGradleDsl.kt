package com.apollographql.apollo.gradle.api

/**
 * Marker for the Apollo Gradle plugin DSL.
 *
 * Prevents accessing outer scopes in nested blocks without explicit receivers (such as `this@apollo`).
 */
@DslMarker
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class ApolloGradleDsl
