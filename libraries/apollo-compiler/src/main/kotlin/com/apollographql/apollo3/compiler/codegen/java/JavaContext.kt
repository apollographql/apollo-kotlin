package com.apollographql.apollo3.compiler.codegen.java

import com.apollographql.apollo3.compiler.GeneratedMethod
import com.apollographql.apollo3.compiler.JavaNullable
import com.apollographql.apollo3.compiler.codegen.PropertyNameGenerator

internal class JavaSchemaContext(
    val layout: JavaSchemaCodegenLayout,
    override val resolver: JavaResolver,
    override val generateMethods: List<GeneratedMethod>,
    override val nullableFieldStyle: JavaNullable,
    override val decapitalizeFields: Boolean,
): JavaContext

internal interface JavaContext : PropertyNameGenerator {
  val resolver: JavaResolver
  val generateMethods: List<GeneratedMethod>
  val nullableFieldStyle: JavaNullable
}

internal class JavaOperationsContext(
    val layout: JavaOperationsCodegenLayout,
    val generateModelBuilders: Boolean,
    override val resolver: JavaResolver,
    override val generateMethods: List<GeneratedMethod>,
    override val nullableFieldStyle: JavaNullable,
    override val decapitalizeFields: Boolean,
): JavaContext
