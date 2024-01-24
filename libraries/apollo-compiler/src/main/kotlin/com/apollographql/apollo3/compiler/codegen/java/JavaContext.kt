package com.apollographql.apollo3.compiler.codegen.java

import com.apollographql.apollo3.compiler.GeneratedMethod
import com.apollographql.apollo3.compiler.JavaNullable
import com.apollographql.apollo3.compiler.codegen.CodegenLayout
import com.apollographql.apollo3.compiler.internal.escapeJavaReservedWord

internal class JavaContext(
    val layout: CodegenLayout,
    val resolver: JavaResolver,
    val generateMethods: List<GeneratedMethod>,
    val generateModelBuilders: Boolean,
    val nullableFieldStyle: JavaNullable,
)

internal fun CodegenLayout.javaPropertyName(name: String) = propertyName(name).escapeJavaReservedWord()
