package com.apollographql.apollo3.compiler.codegen.java.adapter

import com.apollographql.apollo3.compiler.codegen.Identifier
import com.apollographql.apollo3.compiler.codegen.Identifier.serializeDataContext
import com.apollographql.apollo3.compiler.codegen.Identifier.serializeVariables
import com.apollographql.apollo3.compiler.codegen.Identifier.toJson
import com.apollographql.apollo3.compiler.codegen.Identifier.value
import com.apollographql.apollo3.compiler.codegen.Identifier.writer
import com.apollographql.apollo3.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo3.compiler.codegen.java.JavaContext
import com.apollographql.apollo3.compiler.codegen.java.L
import com.apollographql.apollo3.compiler.codegen.java.S
import com.apollographql.apollo3.compiler.codegen.java.T
import com.apollographql.apollo3.compiler.codegen.java.helpers.NamedType
import com.apollographql.apollo3.compiler.codegen.java.helpers.beginOptionalControlFlow
import com.apollographql.apollo3.compiler.codegen.java.helpers.suppressDeprecatedAnnotation
import com.apollographql.apollo3.compiler.ir.IrBooleanValue
import com.apollographql.apollo3.compiler.ir.isOptional
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

internal fun List<NamedType>.variableAdapterTypeSpec(
    context: JavaContext,
    adapterName: String,
    adaptedTypeName: TypeName,
): TypeSpec {
  return TypeSpec.enumBuilder(adapterName)
      .addModifiers(Modifier.PUBLIC)
      .addEnumConstant("INSTANCE")
      .addSuperinterface(ParameterizedTypeName.get(JavaClassNames.VariablesAdapter, adaptedTypeName))
      .addMethod(writeToResponseMethodSpec(context, adaptedTypeName))
      .apply {
        if (this@variableAdapterTypeSpec.any { it.deprecationReason != null }) {
          addAnnotation(suppressDeprecatedAnnotation())
        }
      }
      .build()
}

private fun List<NamedType>.writeToResponseMethodSpec(
    context: JavaContext,
    adaptedTypeName: TypeName,
): MethodSpec {
  return MethodSpec.methodBuilder(serializeVariables)
      .addModifiers(Modifier.PUBLIC)
      .addException(JavaClassNames.IOException)
      .addAnnotation(JavaClassNames.Override)
      .addParameter(JavaClassNames.JsonWriter, writer)
      .addParameter(adaptedTypeName, value)
      .addParameter(JavaClassNames.SerializeVariablesContext, Identifier.context)
      .addCode(writeToResponseCodeBlock(context))
      .build()
}

private fun List<NamedType>.writeToResponseCodeBlock(context: JavaContext): CodeBlock {
  val builder = CodeBlock.builder()
  builder.addStatement("$T $serializeDataContext = new $T(${Identifier.context}.${Identifier.scalarAdapters})", JavaClassNames.DataSerializeContext, JavaClassNames.DataSerializeContext)
  forEach {
    builder.add(it.writeToResponseCodeBlock(context))
  }
  return builder.build()
}

private fun NamedType.writeToResponseCodeBlock(context: JavaContext): CodeBlock {
  val adapterInitializer = context.resolver.adapterInitializer(type, false)
  val builder = CodeBlock.builder()
  val propertyName = context.layout.propertyName(graphQlName)

  if (type.isOptional()) {
    builder.beginOptionalControlFlow(propertyName, context.nullableFieldStyle)
  }

  builder.add("$writer.name($S);\n", graphQlName)
  builder.addStatement("$L.$toJson($writer, $value.$propertyName, $serializeDataContext)", adapterInitializer)
  if (type.isOptional()) {
    builder.endControlFlow()
    if (defaultValue is IrBooleanValue) {
      builder.beginControlFlow("else if (${Identifier.context}.withDefaultBooleanValues)")
      builder.addStatement("$writer.name($S)", graphQlName)
      builder.addStatement("$L.$toJson($writer, $L, $serializeDataContext)", CodeBlock.of("$T.$L", JavaClassNames.Adapters, "BooleanApolloAdapter"), defaultValue.value)
      builder.endControlFlow()
    }
  }

  return builder.build()
}
