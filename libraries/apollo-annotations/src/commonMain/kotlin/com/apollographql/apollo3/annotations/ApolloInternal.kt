package com.apollographql.apollo.annotations

@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This API is for internal use only in Apollo"
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.TYPEALIAS)
annotation class ApolloInternal
