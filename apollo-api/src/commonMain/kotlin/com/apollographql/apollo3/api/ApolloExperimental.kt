package com.apollographql.apollo3.api

@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This API is experimental and can be changed in a backwards-incompatible manner."
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@MustBeDocumented
annotation class ApolloExperimental
