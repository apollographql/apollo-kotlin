package com.apollographql.apollo3.compiler.codegen.adapter

import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.compiler.codegen.Identifier
import com.apollographql.apollo3.compiler.codegen.CgContext
import com.apollographql.apollo3.compiler.codegen.Identifier.typename
import com.apollographql.apollo3.compiler.codegen.helpers.NamedType
import com.apollographql.apollo3.compiler.codegen.helpers.writeToResponseCodeBlock
import com.apollographql.apollo3.compiler.unified.ir.IrModel
import com.apollographql.apollo3.compiler.unified.ir.IrNonNullType
import com.apollographql.apollo3.compiler.unified.ir.IrOptionalType
import com.apollographql.apollo3.compiler.unified.ir.IrProperty
import com.apollographql.apollo3.compiler.unified.ir.isOptional
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.asClassName
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
    hasTypenameArgument: Boolean
): CodeBlock {
  val prefix = model.properties.map { property ->
    val variableIntializer =  when {
      hasTypenameArgument && property.info.responseName == "__typename" -> CodeBlock.of(typename)
      (property.info.type is IrNonNullType && property.info.type.ofType is IrOptionalType) -> CodeBlock.of("%T", Optional.Absent::class.asClassName())
      else -> CodeBlock.of("null")
    }

    CodeBlock.of(
        "var·%L:·%T·=·%L",
        context.layout.variableName(property.info.responseName),
        context.resolver.resolveType(property.info.type).copy(nullable = !property.info.type.isOptional()),
        variableIntializer
    )
  }.joinToCode(separator = "\n", suffix = "\n")

  val loop = CodeBlock.builder()
      .beginControlFlow("while(true)")
      .beginControlFlow("when·(${Identifier.reader}.selectName(${Identifier.RESPONSE_NAMES}))")
      .add(
          model.properties.mapIndexed { index, property ->
            CodeBlock.of(
                "%L·->·%L·=·%L.${Identifier.fromJson}(${Identifier.reader}, ${Identifier.customScalarAdapters})",
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
        val maybeAssertNotNull = if (property.info.type is IrNonNullType && !property.info.type.isOptional()) "!!" else ""
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

private fun IrProperty.toNamedType(): NamedType {
  return NamedType(
      graphQlName = info.responseName,
      description = info.description,
      deprecationReason = info.deprecationReason,
      type = info.type
  )
}
internal fun writeToResponseCodeBlock(model: IrModel, context: CgContext): CodeBlock {
  return model.properties.map { it.toNamedType() }.writeToResponseCodeBlock(context)
}

internal fun ClassName.Companion.from(path: List<String>) = ClassName(
    packageName = path.first(),
    path.drop(1)
)

internal fun CodeBlock.obj(buffered: Boolean): CodeBlock {
  return CodeBlock.Builder()
      .add("%L", this)
      .add(
          ".%M(%L)",
          MemberName("com.apollographql.apollo3.api", "obj"),
          if (buffered) "true" else ""
      ).build()
}