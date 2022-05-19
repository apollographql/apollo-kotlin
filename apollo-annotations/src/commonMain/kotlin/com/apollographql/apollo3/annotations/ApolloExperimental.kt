package com.apollographql.apollo3.annotations

@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "Apollo: This API is experimental and can be changed in a backwards-incompatible manner."
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@MustBeDocumented
annotation class ApolloExperimental
