package com.apollographql.apollo3.annotations

@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "The `UNKNOWN__` class represents GraphQL enums that are not present in the schema and whose `rawValue` cannot be checked at build time. You may want to update your schema instead of calling this directly."
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CONSTRUCTOR)
annotation class ApolloUnknownEnum
