package com.apollographql.apollo3.annotations

@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "Applied to the constructor of an enum's UNKNOWN__ value, as instantiating it is usually a mistake."
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CONSTRUCTOR)
annotation class ApolloEnumConstructor
