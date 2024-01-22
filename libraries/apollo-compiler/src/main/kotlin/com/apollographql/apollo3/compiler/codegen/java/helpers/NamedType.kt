package com.apollographql.apollo3.compiler.codegen.java.helpers

import com.apollographql.apollo3.compiler.JavaNullable
import com.apollographql.apollo3.compiler.codegen.Identifier.value
import com.apollographql.apollo3.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo3.compiler.codegen.java.JavaContext
import com.apollographql.apollo3.compiler.codegen.java.T
import com.apollographql.apollo3.compiler.internal.escapeJavaReservedWord
import com.apollographql.apollo3.compiler.ir.IrInputField
import com.apollographql.apollo3.compiler.ir.IrType
import com.apollographql.apollo3.compiler.ir.IrVariable
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.ParameterSpec

internal class NamedType(
    val graphQlName: String,
    val description: String?,
    val deprecationReason: String?,
    val type: IrType,
)


internal fun NamedType.toParameterSpec(context: JavaContext): ParameterSpec {
  val irType = context.resolver.resolveIrType(type)
  return ParameterSpec
      .builder(
          irType.withoutAnnotations(),
          context.layout.propertyName(graphQlName).escapeJavaReservedWord(),
      )
      .addAnnotations(irType.annotations)
      .build()
}


internal fun IrInputField.toNamedType() = NamedType(
    graphQlName = name,
    type = type,
    description = description,
    deprecationReason = deprecationReason,
)

internal fun IrVariable.toNamedType() = NamedType(
    graphQlName = name,
    type = type,
    description = null,
    deprecationReason = null,
)

internal fun CodeBlock.Builder.beginOptionalControlFlow(propertyName: String, nullableFieldStyle: JavaNullable) {
  when (nullableFieldStyle) {
    JavaNullable.JAVA_OPTIONAL,
    JavaNullable.GUAVA_OPTIONAL,
    -> beginControlFlow("if ($value.$propertyName.isPresent())")

    else -> beginControlFlow("if ($value.$propertyName instanceof $T)", JavaClassNames.Present)
  }
}
