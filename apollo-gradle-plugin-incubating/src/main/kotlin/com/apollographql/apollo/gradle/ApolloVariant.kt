package com.apollographql.apollo.gradle

import org.gradle.api.file.SourceDirectorySet

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
    val sourceSetNames: List<String>

)