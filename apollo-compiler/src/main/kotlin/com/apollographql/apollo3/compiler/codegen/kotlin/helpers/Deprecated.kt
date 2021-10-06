package com.apollographql.apollo3.compiler.codegen.kotlin.helpers

import com.squareup.kotlinpoet.AnnotationSpec

internal fun deprecatedAnnotation(message: String) = AnnotationSpec
    .builder(Deprecated::class)
    .apply {
      if (message.isNotBlank()) {
        addMember("message = %S", message)
      }
    }
    .build()