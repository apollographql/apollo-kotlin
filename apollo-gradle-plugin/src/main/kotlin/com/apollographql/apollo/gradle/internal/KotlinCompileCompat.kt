package com.apollographql.apollo.gradle.internal

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// From https://github.com/square/wire/pull/2273:
// The signature of this function changed in Kotlin 1.7, so we invoke it reflectively
// to support both.
// 1.6.x: `fun source(vararg sources: Any): SourceTask
// 1.7.x: `fun source(vararg sources: Any)
fun KotlinCompile.invokeSource(source: Any) {
  SOURCE_FUNCTION.invoke(this, arrayOf(source))
}

private val SOURCE_FUNCTION = KotlinCompile::class.java
    .getMethod("source", java.lang.reflect.Array.newInstance(Any::class.java, 0).javaClass)
