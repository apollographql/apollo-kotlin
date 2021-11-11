package com.apollographql.apollo3.compiler.codegen.kotlin.helpers

import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinClassNames
import com.squareup.kotlinpoet.AnnotationSpec

internal fun deprecatedAnnotation(message: String) = AnnotationSpec
    .builder(KotlinClassNames.Deprecated)
    .apply {
      if (message.isNotBlank()) {
        addMember("message = %S", message)
      }
    }
    .build()