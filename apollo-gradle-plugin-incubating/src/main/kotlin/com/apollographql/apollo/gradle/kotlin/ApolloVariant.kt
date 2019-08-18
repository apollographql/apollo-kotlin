package com.apollographql.apollo.gradle.kotlin

import org.gradle.api.file.SourceDirectorySet

class ApolloVariant(
    /**
     * The full name of the variant
     *
     * For an example for the demoDebug variant: `demoDebug`
     */
    val name: String,

    /**
     * The sourceDirectorySet used by the compilation tasks containing all the java/kotlin sources for this variant
     * This is used to register our own generated apollo models so they're picked up by the java/kotlin compiler
     *
     * For an example for the demoDebug variant:
     *
     * src/demoDebug/java
     * src/demo/java
     * src/main/kotlin
     * src/main/java
     * build/generated/source/kapt/demoDebug/
     * etc...
     *
     * To determine where to look for .graphql files, we're using sourceSetNames
     */
    //val sourceDirectorySet: SourceDirectorySet,

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