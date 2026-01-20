package com.apollographql.apollo.gradle

import com.apollographql.apollo.annotations.ApolloInternal

/**
 * A substitute for Kotlin friend paths that works well in the IDE.
 * Symbols annotated with [EmbeddedGradleSymbol] must be embedded in a fat jar so that they are not visible
 * for outside consumers.
 *
 * See https://youtrack.jetbrains.com/issue/KTIJ-30664.
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This API should not be visible outside the Apollo codebase, file a bug if it is."
)
@Retention(AnnotationRetention.BINARY)
@ApolloInternal
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.TYPEALIAS)
annotation class EmbeddedGradleSymbol