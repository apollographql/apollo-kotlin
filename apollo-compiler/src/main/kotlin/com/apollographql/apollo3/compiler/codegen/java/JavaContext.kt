package com.apollographql.apollo3.compiler.codegen.java

import com.apollographql.apollo3.compiler.JavaNullable

internal class JavaContext(
    val layout: JavaCodegenLayout,
    val resolver: JavaResolver,
    val generateModelBuilder: Boolean,
    val nullableFieldStyle: JavaNullable,
)
