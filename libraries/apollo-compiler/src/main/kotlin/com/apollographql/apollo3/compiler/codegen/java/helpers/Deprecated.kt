package com.apollographql.apollo3.compiler.codegen.java.helpers

import com.apollographql.apollo3.compiler.codegen.java.JavaClassNames
import com.squareup.javapoet.AnnotationSpec

internal fun deprecatedAnnotation() = AnnotationSpec
    .builder(JavaClassNames.Deprecated)
    .build()