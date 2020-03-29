package com.apollographql.apollo.compiler.codegen.kotlin

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec

val MULTIPLATFORM_THROWS = ClassName.bestGuess("com.apollographql.apollo.api.internal.Throws")
val MULTIPLATFORM_IO_EXCEPTION = ClassName.bestGuess("okio.IOException")

fun FunSpec.Builder.throwsMultiplatformIOException() = throws(MULTIPLATFORM_IO_EXCEPTION)

/**
 * User instead of Kotlin poet throws since we have our own Throws class for Kotlin Multiplatform
 */
fun FunSpec.Builder.throws(vararg exceptionClasses: ClassName) = addAnnotation(
    AnnotationSpec.builder(MULTIPLATFORM_THROWS)
        .apply { exceptionClasses.forEach { addMember("%T::class", it) } }
        .build())
