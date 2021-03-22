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

private fun List<NamedType>.prefixCode(): CodeBlock {
  return map { namedType ->
    CodeBlock.of(
        "var·%L:·%T·=·%L",
        kotlinNameForVariable(namedType.graphQlName),
        namedType.typeName().copy(nullable = true),
    )
  }.joinToCode(separator = "\n", suffix = "\n")
}

private fun List<NamedType>.loopCode(): CodeBlock {
  return CodeBlock.builder()
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
}

internal fun List<NamedType>.readFromResponseCode(
    adaptedTypeName: TypeName,
): CodeBlock {
  val prefix = prefixCode()

  val loop = loopCode()

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
      .addCode(readFromResponseCode(adaptedTypeName))
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
      .addCode(writeToResponseCode())
      .build()
}


internal fun List<NamedType>.writeToResponseCode(): CodeBlock {
  val builder = CodeBlock.builder()
  forEach {
    builder.add(it.writeToResponseCode())
  }
  return builder.build()
}

private fun NamedType.writeToResponseCode(): CodeBlock {
  return CodeBlock.builder().apply {
    addStatement("${Identifier.writer}.name(%S)", graphQlName)
    addStatement(
        "%L.${Identifier.toResponse}(${Identifier.writer}, ${Identifier.responseAdapterCache}, value.${kotlinNameForProperty(graphQlName)})",
        adapterInitializer()
    )
  }.build()
}