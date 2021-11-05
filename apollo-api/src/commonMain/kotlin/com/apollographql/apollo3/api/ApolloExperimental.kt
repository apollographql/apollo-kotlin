package com.apollographql.apollo3.api

@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This API is experimental and subject to change, use with caution"
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@MustBeDocumented
annotation class ApolloExperimental
