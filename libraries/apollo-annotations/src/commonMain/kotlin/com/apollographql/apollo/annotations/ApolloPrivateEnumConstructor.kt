package com.apollographql.apollo.annotations

/**
 * Kotlin has no static factory functions like Java so we rely on an OptIn marker to prevent public usage.
 * See https://youtrack.jetbrains.com/issue/KT-19400/Allow-access-to-private-members-between-nested-classes-of-the-same-class
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "The `__UNKNOWN` constructor is public for technical reasons only. Use `${'$'}YourEnum.safeValueOf(String)` instead."
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CONSTRUCTOR)
annotation class ApolloPrivateEnumConstructor