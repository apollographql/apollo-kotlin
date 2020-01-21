package com.apollographql.apollo.gradle.internal

class ApolloVariant(
    /**
     * The full name of the variant
     *
     * For an example for the demoDebug variant: `demoDebug`
     */
    val name: String,

    /**
     * A list of root sourceSets names where to look for .graphql files. We're not using sourceDirectorySet as
     * we don't want to look into generated files.
     * They are sorted in the same order as Android variant.sourceSets so the last one overrides the ones before
     *
     * For an example for the demoDebug variant:
     *
     * main
     * demo
     * demoDebug
     * etc...
     *
     */
    val sourceSetNames: List<String>,

    /**
     * The androidVariant if any. This can be used to link other tasks/plugins in your build logic.
     * This is Any so that it does not pull a dependency on the gradle plugin but can be safely cast
     * to BaseVariant.
     */
    val androidVariant: Any?,

    /**
     * True if this variant is a test variant.
     */
    val isTest: Boolean
)