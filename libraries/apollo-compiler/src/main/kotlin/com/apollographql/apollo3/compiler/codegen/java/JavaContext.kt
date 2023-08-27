package com.apollographql.apollo3.compiler.codegen.java

import com.apollographql.apollo3.compiler.GeneratedMethod
import com.apollographql.apollo3.compiler.JavaNullable

internal class JavaContext(
    val layout: JavaCodegenLayout,
    val resolver: JavaResolver,
    val generateMethods: List<GeneratedMethod>,
    val generateModelBuilders: Boolean,
    val nullableFieldStyle: JavaNullable,
)
