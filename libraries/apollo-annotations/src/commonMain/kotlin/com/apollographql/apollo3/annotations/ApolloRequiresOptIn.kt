package com.apollographql.apollo.annotations

@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This field, input field or enum value is declared `@requiresOptIn`."
)
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
@MustBeDocumented
annotation class ApolloRequiresOptIn
