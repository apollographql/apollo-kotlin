package com.apollographql.apollo.compiler.codegen.java.helpers

import com.apollographql.apollo.compiler.codegen.java.JavaClassNames
import com.squareup.javapoet.AnnotationSpec

internal fun deprecatedAnnotation() = AnnotationSpec
    .builder(JavaClassNames.Deprecated)
    .build()

internal fun suppressDeprecatedAnnotation() = suppressAnnotation("deprecation")
