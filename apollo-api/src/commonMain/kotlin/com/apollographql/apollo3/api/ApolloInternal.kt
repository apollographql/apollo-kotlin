package com.apollographql.apollo3.api

/**
 * Marks declarations that are not to be used outside the apollo-android repo
 * They would typically be `internal` but tests require them and they marked with this annotation instead
 * Do not use them unless writing tests
 */
@Retention(value = AnnotationRetention.BINARY)
@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
annotation class ApolloInternal()
