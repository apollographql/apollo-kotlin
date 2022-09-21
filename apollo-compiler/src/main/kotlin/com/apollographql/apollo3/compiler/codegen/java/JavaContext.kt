package com.apollographql.apollo3.compiler.codegen.java

import com.apollographql.apollo3.compiler.JavaNullableFieldStyle

internal class JavaContext(
    val layout: JavaCodegenLayout,
    val resolver: JavaResolver,
    val generateModelBuilder: Boolean,
    val nullableFieldStyle: JavaNullableFieldStyle,
)
