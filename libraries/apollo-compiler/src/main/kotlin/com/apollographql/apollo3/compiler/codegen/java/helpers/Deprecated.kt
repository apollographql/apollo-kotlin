package com.apollographql.apollo3.compiler.codegen.java.helpers

import com.apollographql.apollo3.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo3.compiler.codegen.java.S
import com.squareup.javapoet.AnnotationSpec

internal fun deprecatedAnnotation() = AnnotationSpec
    .builder(JavaClassNames.Deprecated)
    .build()

internal fun suppressDeprecatedAnnotation() = AnnotationSpec.builder(JavaClassNames.SuppressWarnings)
    .addMember("value", S, "deprecation")
    .build()