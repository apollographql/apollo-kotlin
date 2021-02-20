package com.apollographql.apollo3.compiler.backend.codegen

import com.apollographql.apollo3.api.Input
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.internal.ResponseAdapter
import com.apollographql.apollo3.api.internal.json.JsonReader
import com.apollographql.apollo3.api.internal.json.JsonWriter
import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.backend.ast.CodeGenerationAst
import com.apollographql.apollo3.compiler.escapeKotlinReservedWord
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName

internal fun CodeGenerationAst.InputField.asInputTypeName() = if (isRequired) {
  type.asTypeName()
} else {
  Input::class.asClassName().parameterizedBy(type.asTypeName().copy(nullable = false))
}

internal fun CodeGenerationAst.InputField.toParameterSpec(): ParameterSpec {
  return ParameterSpec
      .builder(
          name = name.escapeKotlinReservedWord(),
          type = asInputTypeName()
      )
      .applyIf(description.isNotBlank()) { addKdoc("%L\n", description) }
      .applyIf(!isRequired) { defaultValue("%T()", Input.Absent::class.asClassName()) }
      .build()
}

fun serializeVariablesFunSpec(
    funName: String,
    packageName: String,
    name: String,
): FunSpec {
  val serializerClassName = ClassName("$packageName.adapter", kotlinNameForSerializer(name))
  val body = CodeBlock.builder().apply {
    addStatement("${Identifier.RESPONSE_ADAPTER_CACHE}.getVariablesAdapterFor(this::class) {")
    indent()
    addStatement("%T(${Identifier.RESPONSE_ADAPTER_CACHE})", serializerClassName)
    unindent()
    addStatement("}.toResponse(writer, this)")
  }.build()

  return FunSpec.builder(funName)
      .addModifiers(KModifier.OVERRIDE)
      .addParameter("writer", JsonWriter::class)
      .addParameter(ParameterSpec.builder(Identifier.RESPONSE_ADAPTER_CACHE, ResponseAdapterCache::class.asTypeName()).build())
      .addCode(body)
      .build()

}

fun notImplementedFromResponseFunSpec(returnTypeName: TypeName) = FunSpec.builder("fromResponse")
    .addModifiers(KModifier.OVERRIDE)
    .addParameter(ParameterSpec.builder(Identifier.READER, JsonReader::class).build())
    .returns(returnTypeName)
    .addCode("throw %T(%S)", ClassName("kotlin", "IllegalStateException"), "Input type used in output position")
    .build()

private fun CodeGenerationAst.InputField.actualType() = if (isRequired) {
  type.nonNullable()
} else {
  type
}

internal fun List<CodeGenerationAst.InputField>.serializerTypeSpec(
    packageName: String,
    name: String,
    generateAsInternal: Boolean
): TypeSpec {
  val className = ClassName(packageName, name)
  val builder = TypeSpec.classBuilder(kotlinNameForSerializer(name))

  if (generateAsInternal) {
    builder.addModifiers(KModifier.INTERNAL)
  }
  builder.addSuperinterface(ResponseAdapter::class.asClassName().parameterizedBy(className))

  builder.primaryConstructor(FunSpec.constructorBuilder()
      .addParameter(Identifier.RESPONSE_ADAPTER_CACHE, ResponseAdapterCache::class)
      .build()
  )

  map {
    it.actualType()
  }.distinct()
      .forEach {
        builder.addProperty(it.adapterPropertySpec())
      }

  builder.addFunction(notImplementedFromResponseFunSpec(className))
  builder.addFunction(FunSpec.builder(Identifier.TO_RESPONSE)
      .addParameter(Identifier.WRITER, JsonWriter::class)
      .addParameter(Identifier.VALUE, className)
      .addModifiers(KModifier.OVERRIDE)
      .addCode(CodeBlock.Builder().apply {
        addStatement("writer.beginObject()")
        forEach {
          if (!it.isRequired) {
            beginControlFlow("if (value.%L is %T)", kotlinNameForVariable(it.name), Input.Present::class)
            addStatement("writer.name(%S)", it.name)
            addStatement("%L.toResponse(writer, value.%L.value)", kotlinNameForAdapterField(it.actualType()), kotlinNameForVariable(it.name))
            endControlFlow()
          } else {
            addStatement("writer.name(%S)", it.name)
            addStatement("%L.toResponse(writer, value.%L)", kotlinNameForAdapterField(it.actualType()), kotlinNameForVariable(it.name))
          }
        }
        addStatement("writer.endObject()")
      }.build())
      .build()
  )

  return builder.build()
}
