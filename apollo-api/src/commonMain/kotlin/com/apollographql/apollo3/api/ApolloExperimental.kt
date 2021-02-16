package com.apollographql.apollo3.api

/**
 * Marks declarations that are still **experimental**.
 * Declarations marked with this annotation are unstable and subject to change.
 */
@MustBeDocumented
@Retention(value = AnnotationRetention.BINARY)
@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
annotation class ApolloExperimental
