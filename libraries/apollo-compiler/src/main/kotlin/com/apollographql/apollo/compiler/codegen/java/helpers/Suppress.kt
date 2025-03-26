package com.apollographql.apollo.compiler.codegen.java.helpers

import com.apollographql.apollo.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo.compiler.codegen.java.S
import com.squareup.javapoet.AnnotationSpec

internal fun suppressAnnotation(value: String) = AnnotationSpec.builder(JavaClassNames.SuppressWarnings)
    .addMember("value", S, value)
    .build()
