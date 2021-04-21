package com.apollographql.apollo3.compiler.unified.codegen.adapter

import com.apollographql.apollo3.compiler.backend.codegen.Identifier
import com.apollographql.apollo3.compiler.backend.codegen.Identifier.responseAdapterCache
import com.apollographql.apollo3.compiler.backend.codegen.Identifier.toResponse
import com.apollographql.apollo3.compiler.backend.codegen.Identifier.value
import com.apollographql.apollo3.compiler.backend.codegen.Identifier.writer
import com.apollographql.apollo3.compiler.backend.codegen.kotlinNameForVariable
import com.apollographql.apollo3.compiler.unified.codegen.CgContext
import com.apollographql.apollo3.compiler.unified.ir.IrModel
import com.apollographql.apollo3.compiler.unified.ir.IrNonNullType
import com.apollographql.apollo3.compiler.unified.ir.IrProperty
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.joinToCode

internal fun responseNamesPropertySpec(model: IrModel): PropertySpec {
  val initializer = model.properties.map {
    CodeBlock.of("%S", it.info.responseName)
  }.joinToCode(prefix = "listOf(", separator = ", ", suffix = ")")

  return PropertySpec.builder(Identifier.RESPONSE_NAMES, List::class.parameterizedBy(String::class))
      .initializer(initializer)
      .build()
}

internal fun readFromResponseCodeBlock(
    model: IrModel,
    context: CgContext,
    variableInitializer: (String) -> String
): CodeBlock {
  val prefix = model.properties.map { property ->
    CodeBlock.of(
        "var·%L:·%T·=·%L",
        kotlinNameForVariable(property.info.responseName),
        context.resolver.resolveType(property.info.type).copy(nullable = true),
        variableInitializer(property.info.responseName)
    )
  }.joinToCode(separator = "\n", suffix = "\n")

  val loop = CodeBlock.builder()
      .beginControlFlow("while(true)")
      .beginControlFlow("when·(${Identifier.reader}.selectName(${Identifier.RESPONSE_NAMES}))")
      .add(
          model.properties.mapIndexed { index, property ->
            CodeBlock.of(
                "%L·->·%L·=·%L.${Identifier.fromResponse}(${Identifier.reader}, ${Identifier.responseAdapterCache})",
                index,
                context.layout.variableName(property.info.responseName),
                context.resolver.adapterInitializer(property.info.type)
            )
          }.joinToCode(separator = "\n", suffix = "\n")
      )
      .addStatement("else -> break")
      .endControlFlow()
      .endControlFlow()
      .build()

  val suffix = CodeBlock.builder()
      .addStatement("return·%T(", context.resolver.resolveModel(model.id))
      .indent()
      .add(model.properties.map { property ->
        val maybeAssertNotNull = if (property.info.type is IrNonNullType) "!!" else ""
        CodeBlock.of(
            "%L·=·%L%L",
            context.layout.propertyName(property.info.responseName),
            context.layout.variableName(property.info.responseName),
            maybeAssertNotNull
        )
      }.joinToCode(separator = ",\n", suffix = "\n"))
      .unindent()
      .addStatement(")")
      .build()

  return CodeBlock.builder()
      .add(prefix)
      .add(loop)
      .add(suffix)
      .build()
}

internal fun writeToResponseCodeBlock(model: IrModel, context: CgContext): CodeBlock {
  val builder = CodeBlock.builder()
  model.properties.forEach {
    builder.add(writeToResponseCodeBlock(it, context))
  }
  return builder.build()
}

internal fun writeToResponseCodeBlock(property: IrProperty, context: CgContext): CodeBlock {
  return CodeBlock.builder().apply {
    val propertyName = context.layout.propertyName(property.info.responseName)
    addStatement("$writer.name(%S)", property.info.responseName)
    addStatement(
        "%L.$toResponse($writer, $responseAdapterCache, $value.$propertyName)",
        context.resolver.adapterInitializer(property.info.type)
    )
  }.build()
}

internal fun ClassName.Companion.from(path: List<String>) = ClassName(
    packageName = path.first(),
    path.drop(1)
)