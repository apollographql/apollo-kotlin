package com.apollographql.apollo.compiler.codegen.kotlin.helpers

import com.apollographql.apollo.compiler.codegen.kotlin.KotlinSymbols
import com.squareup.kotlinpoet.AnnotationSpec

internal fun deprecatedAnnotation(message: String) = AnnotationSpec
    .builder(KotlinSymbols.Deprecated)
    .apply {
      addMember("message = %S", message)
    }
    .build()