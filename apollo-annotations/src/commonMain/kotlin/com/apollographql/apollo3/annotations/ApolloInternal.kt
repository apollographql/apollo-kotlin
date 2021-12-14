package com.apollographql.apollo3.annotations

@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This API is for internal use only in Apollo"
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class ApolloInternal
