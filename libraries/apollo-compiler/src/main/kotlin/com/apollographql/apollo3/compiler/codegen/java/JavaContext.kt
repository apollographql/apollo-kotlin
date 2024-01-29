package com.apollographql.apollo3.compiler.codegen.java

import com.apollographql.apollo3.compiler.GeneratedMethod
import com.apollographql.apollo3.compiler.JavaNullable
import com.apollographql.apollo3.compiler.codegen.SchemaAndOperationsLayoutImpl
import com.apollographql.apollo3.compiler.internal.escapeJavaReservedWord

internal class JavaContext(
    val layout: SchemaAndOperationsLayoutImpl,
    val resolver: JavaResolver,
    val generateMethods: List<GeneratedMethod>,
    val generateModelBuilders: Boolean,
    val nullableFieldStyle: JavaNullable,
)

internal fun SchemaAndOperationsLayoutImpl.javaPropertyName(name: String) = propertyName(name).escapeJavaReservedWord()
