package com.apollographql.apollo.compiler.codegen.kotlin

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec

private val MULTIPLATFORM_THROWS = ClassName("com.apollographql.apollo.api.internal", "Throws")
private val MULTIPLATFORM_IO_EXCEPTION = ClassName("okio", "IOException")

fun FunSpec.Builder.throwsMultiplatformIOException() = throws(MULTIPLATFORM_IO_EXCEPTION)

/**
 * User instead of Kotlin poet throws since we have our own Throws class for Kotlin Multiplatform
 */
fun FunSpec.Builder.throws(vararg exceptionClasses: ClassName) = addAnnotation(
    AnnotationSpec.builder(MULTIPLATFORM_THROWS)
        .apply { exceptionClasses.forEach { addMember("%T::class", it) } }
        .build())
