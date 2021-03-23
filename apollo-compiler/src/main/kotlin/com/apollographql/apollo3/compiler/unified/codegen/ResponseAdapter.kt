package com.apollographql.apollo3.compiler.unified.codegen

import com.apollographql.apollo3.api.ResponseAdapter
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.compiler.backend.codegen.Identifier
import com.apollographql.apollo3.compiler.backend.codegen.kotlinNameForProperty
import com.apollographql.apollo3.compiler.backend.codegen.kotlinNameForVariable
import com.apollographql.apollo3.compiler.unified.codegen.helpers.NamedType
import com.apollographql.apollo3.compiler.unified.codegen.helpers.adapterInitializer
import com.apollographql.apollo3.compiler.unified.codegen.helpers.typeName
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.joinToCode

internal fun List<NamedType>.responseNamesPropertySpec(): PropertySpec {
  val initializer = map {
    CodeBlock.of("%S", it.graphQlName)
  }.joinToCode(prefix = "listOf(", separator = ", ", suffix = ")")

  return PropertySpec.builder("RESPONSE_NAMES", List::class.parameterizedBy(String::class))
      .initializer(initializer)
      .build()
}

internal fun List<NamedType>.adapterTypeSpec(
    adapterName: String,
    adaptedTypeName: TypeName,
): TypeSpec {
  return TypeSpec.objectBuilder(adapterName)
      .addSuperinterface(ResponseAdapter::class.asTypeName().parameterizedBy(adaptedTypeName))
      .addProperty(responseNamesPropertySpec())
      .addFunction(readFromResponseFunSpec(adaptedTypeName))
      .addFunction(writeToResponseFunSpec(adaptedTypeName))
      .build()
}

internal fun List<NamedType>.inputOnlyAdapterTypeSpec(
    adapterName: String,
    adaptedTypeName: TypeName,
): TypeSpec {
  return TypeSpec.objectBuilder(adapterName)
      .addSuperinterface(ResponseAdapter::class.asTypeName().parameterizedBy(adaptedTypeName))
      .addProperty(responseNamesPropertySpec())
      .addFunction(readFromResponseFunSpec(adaptedTypeName))
      .addFunction(notImplementedFromResponseFunSpec(adaptedTypeName))
      .build()
}

private fun notImplementedFromResponseFunSpec(adaptedTypeName: TypeName) = FunSpec.builder("fromResponse")
    .addModifiers(KModifier.OVERRIDE)
    .addParameter(Identifier.reader, JsonReader::class)
    .addParameter(Identifier.responseAdapterCache, ResponseAdapterCache::class.asTypeName())
    .returns(adaptedTypeName)
    .addCode("throw %T(%S)", ClassName("kotlin", "IllegalStateException"), "Input type used in output position")
    .build()

internal fun List<NamedType>.readFromResponseBlockCode(
    adaptedTypeName: TypeName,
): CodeBlock {
  val prefix = map { namedType ->
    CodeBlock.of(
        "var·%L:·%T·=·%L",
        kotlinNameForVariable(namedType.graphQlName),
        namedType.typeName().copy(nullable = true),
    )
  }.joinToCode(separator = "\n", suffix = "\n")

  val loop = CodeBlock.builder()
      .beginControlFlow("while(true)")
      .beginControlFlow("when·(${Identifier.reader}.selectName(RESPONSE_NAMES))")
      .add(
          mapIndexed { fieldIndex, typedName ->
            CodeBlock.of(
                "%L·->·%L·=·%L.${Identifier.fromResponse}(${Identifier.reader}, ${Identifier.responseAdapterCache})",
                fieldIndex,
                kotlinNameForVariable(typedName.graphQlName),
                typedName.adapterInitializer()
            )
          }.joinToCode(separator = "\n", suffix = "\n")
      )
      .addStatement("else -> break")
      .endControlFlow()
      .endControlFlow()
      .build()

  val suffix = CodeBlock.builder()
      .addStatement("return·%T(", adaptedTypeName)
      .indent()
      .add(map { namedType ->
        CodeBlock.of(
            "%L·=·%L%L",
            kotlinNameForProperty(namedType.graphQlName),
            kotlinNameForVariable(namedType.graphQlName),
            if (namedType.typeName().isNullable) "" else "!!"
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

fun List<NamedType>.readFromResponseFunSpec(
    adaptedTypeName: TypeName,
): FunSpec {
  return FunSpec.builder(Identifier.fromResponse)
      .returns(adaptedTypeName)
      .addParameter(Identifier.reader, JsonReader::class)
      .addParameter(Identifier.responseAdapterCache, ResponseAdapterCache::class)
      .addModifiers(KModifier.OVERRIDE)
      .addCode(readFromResponseBlockCode(adaptedTypeName))
      .build()
}

private fun List<NamedType>.writeToResponseFunSpec(
    adaptedTypeName: TypeName
): FunSpec {
  return FunSpec.builder(Identifier.toResponse)
      .addModifiers(KModifier.OVERRIDE)
      .addParameter(Identifier.writer, JsonWriter::class.asTypeName())
      .addParameter(Identifier.responseAdapterCache, ResponseAdapterCache::class)
      .addParameter(Identifier.value, adaptedTypeName)
      .addCode(writeToResponseBlockCode())
      .build()
}


internal fun List<NamedType>.writeToResponseBlockCode(): CodeBlock {
  val builder = CodeBlock.builder()
  forEach {
    builder.add(it.writeToResponseBlockCode())
  }
  return builder.build()
}

private fun NamedType.writeToResponseBlockCode(): CodeBlock {
  return CodeBlock.builder().apply {
    addStatement("${Identifier.writer}.name(%S)", graphQlName)
    addStatement(
        "%L.${Identifier.toResponse}(${Identifier.writer}, ${Identifier.responseAdapterCache}, value.${kotlinNameForProperty(graphQlName)})",
        adapterInitializer()
    )
  }.build()
}