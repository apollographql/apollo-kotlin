package com.apollographql.apollo.compiler.codegen.java

import com.apollographql.apollo.compiler.GeneratedMethod
import com.apollographql.apollo.compiler.JavaNullable
import com.apollographql.apollo.compiler.codegen.CommonLayout
import com.apollographql.apollo.compiler.codegen.OperationsLayout
import com.apollographql.apollo.compiler.codegen.SchemaLayout
import com.apollographql.apollo.compiler.internal.escapeJavaReservedWord

internal interface JavaContext {
  val layout: CommonLayout
  val resolver: JavaResolver
  val generateMethods: List<GeneratedMethod>
  val generateModelBuilders: Boolean
  val nullableFieldStyle: JavaNullable
}

internal class JavaSchemaContext(
    override val layout: SchemaLayout,
    override val resolver: JavaResolver,
    override val generateMethods: List<GeneratedMethod>,
    override val generateModelBuilders: Boolean,
    override val nullableFieldStyle: JavaNullable,
): JavaContext

internal class JavaOperationsContext(
    override val layout: OperationsLayout,
    override val resolver: JavaResolver,
    override val generateMethods: List<GeneratedMethod>,
    override val generateModelBuilders: Boolean,
    override val nullableFieldStyle: JavaNullable,
): JavaContext

internal fun CommonLayout.javaPropertyName(name: String) = propertyName(name).escapeJavaReservedWord()
