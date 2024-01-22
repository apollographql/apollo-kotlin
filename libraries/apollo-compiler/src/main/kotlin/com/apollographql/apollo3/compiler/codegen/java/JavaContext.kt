package com.apollographql.apollo3.compiler.codegen.java

import com.apollographql.apollo3.compiler.GeneratedMethod
import com.apollographql.apollo3.compiler.JavaNullable
import com.apollographql.apollo3.compiler.codegen.CodegenLayout

internal class JavaContext(
    val layout: CodegenLayout,
    val resolver: JavaResolver,
    val generateMethods: List<GeneratedMethod>,
    val generateModelBuilders: Boolean,
    val nullableFieldStyle: JavaNullable,
)
