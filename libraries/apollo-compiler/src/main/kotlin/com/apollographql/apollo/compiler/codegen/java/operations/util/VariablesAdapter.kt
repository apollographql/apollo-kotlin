/*
 * Generates ResponseAdapters for variables
 */
package com.apollographql.apollo.compiler.codegen.java.operations.util

import com.apollographql.apollo.compiler.codegen.Identifier.Empty
import com.apollographql.apollo.compiler.codegen.Identifier.customScalarAdapters
import com.apollographql.apollo.compiler.codegen.Identifier.serializeVariables
import com.apollographql.apollo.compiler.codegen.Identifier.toJson
import com.apollographql.apollo.compiler.codegen.Identifier.value
import com.apollographql.apollo.compiler.codegen.Identifier.withDefaultValues
import com.apollographql.apollo.compiler.codegen.Identifier.writer
import com.apollographql.apollo.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo.compiler.codegen.java.JavaContext
import com.apollographql.apollo.compiler.codegen.java.L
import com.apollographql.apollo.compiler.codegen.java.S
import com.apollographql.apollo.compiler.codegen.java.T
import com.apollographql.apollo.compiler.codegen.java.helpers.beginOptionalControlFlow
import com.apollographql.apollo.compiler.codegen.java.helpers.codeBlock
import com.apollographql.apollo.compiler.codegen.java.javaPropertyName
import com.apollographql.apollo.compiler.ir.IrVariable
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

internal fun List<IrVariable>.variableAdapterTypeSpec(
    context: JavaContext,
    adapterName: String,
    adaptedTypeName: TypeName,
): TypeSpec {
  return TypeSpec.enumBuilder(adapterName)
      .addModifiers(Modifier.PUBLIC)
      .addEnumConstant("INSTANCE")
      .addMethod(writeToResponseMethodSpec(context, adaptedTypeName))
      .build()
}

private fun List<IrVariable>.writeToResponseMethodSpec(
    context: JavaContext,
    adaptedTypeName: TypeName,
): MethodSpec {
  return MethodSpec.methodBuilder(serializeVariables)
      .addModifiers(Modifier.PUBLIC)
      .addException(JavaClassNames.IOException)
      .addParameter(JavaClassNames.JsonWriter, writer)
      .addParameter(adaptedTypeName, value)
      .addParameter(JavaClassNames.CustomScalarAdapters, customScalarAdapters)
      .addParameter(TypeName.BOOLEAN, withDefaultValues)
      .addCode(writeToResponseCodeBlock(context))
      .build()
}

private fun List<IrVariable>.writeToResponseCodeBlock(context: JavaContext): CodeBlock {
  val builder = CodeBlock.builder()
  forEach {
    builder.add(it.writeToResponseCodeBlock(context))
  }
  return builder.build()
}

private fun IrVariable.writeToResponseCodeBlock(context: JavaContext): CodeBlock {
  val adapterInitializer = context.resolver.adapterInitializer(type, false)
  val builder = CodeBlock.builder()
  val propertyName = context.layout.javaPropertyName(name)

  if (type.optional) {
    builder.beginOptionalControlFlow(propertyName, context.nullableFieldStyle)
  }

  builder.add("$writer.name($S);\n", name)
  builder.addStatement("$L.$toJson($writer, $customScalarAdapters, $value.$propertyName)", adapterInitializer)
  if (type.optional) {
    builder.endControlFlow()
    if (defaultValue != null) {
      builder.beginControlFlow("else if ($withDefaultValues)")
      builder.addStatement("$writer.name($S)", name)
      builder.addStatement(
          "$L.$toJson($writer, $T.$Empty, $L)",
          CodeBlock.of("$T.$L", JavaClassNames.Adapters, "NullableAnyAdapter"),
          JavaClassNames.CustomScalarAdapters,
          defaultValue.codeBlock(),
      )
      builder.endControlFlow()
    }
  }

  return builder.build()
}
