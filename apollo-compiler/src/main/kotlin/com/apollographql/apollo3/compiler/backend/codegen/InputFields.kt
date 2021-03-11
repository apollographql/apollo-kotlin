package com.apollographql.apollo3.compiler.backend.codegen

import com.apollographql.apollo3.api.Input
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.ResponseAdapter
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
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
  Input::class.asClassName().parameterizedBy(type.asTypeName())
}

internal fun CodeGenerationAst.InputField.toParameterSpec(): ParameterSpec {
  return ParameterSpec
      .builder(
          name = name.escapeKotlinReservedWord(),
          type = asInputTypeName()
      )
      .applyIf(description.isNotBlank()) { addKdoc("%L\n", description) }
      .applyIf(!isRequired) { defaultValue("%T", Input.Absent::class.asClassName()) }
      .build()
}

fun serializeVariablesFunSpec(
    funName: String,
    packageName: String,
    name: String,
): FunSpec {
  val serializerClassName = ClassName("$packageName.adapter", kotlinNameForVariablesAdapter(name))

  return FunSpec.builder(funName)
      .addModifiers(KModifier.OVERRIDE)
      .addParameter("writer", JsonWriter::class)
      .addParameter(Identifier.responseAdapterCache, ResponseAdapterCache::class.asTypeName())
      .addCode("%T.toResponse(writer, ${Identifier.responseAdapterCache}, this)", serializerClassName)
      .build()
}

fun notImplementedFromResponseFunSpec(returnTypeName: TypeName) = FunSpec.builder("fromResponse")
    .addModifiers(KModifier.OVERRIDE)
    .addParameter(Identifier.reader, JsonReader::class)
    .addParameter(Identifier.responseAdapterCache, ResponseAdapterCache::class.asTypeName())
    .returns(returnTypeName)
    .addCode("throw %T(%S)", ClassName("kotlin", "IllegalStateException"), "Input type used in output position")
    .build()

internal fun List<CodeGenerationAst.InputField>.variablesAdapterTypeSpec(
    packageName: String,
    name: String,
    generateAsInternal: Boolean
): TypeSpec {
  return inputFieldsAdapterTypeSpec(
      packageName = packageName,
      name = name,
      adapterName = kotlinNameForVariablesAdapter(name),
      generateAsInternal = generateAsInternal
  )
}

internal fun List<CodeGenerationAst.InputField>.inputObjectAdapterTypeSpec(
    packageName: String,
    name: String,
    generateAsInternal: Boolean
): TypeSpec {
  return inputFieldsAdapterTypeSpec(
      packageName = packageName,
      name = name,
      adapterName = kotlinNameForInputObjectAdapter(name),
      generateAsInternal = generateAsInternal
  )
}

private fun List<CodeGenerationAst.InputField>.inputFieldsAdapterTypeSpec(
    packageName: String,
    name: String,
    adapterName: String,
    generateAsInternal: Boolean
): TypeSpec {
  val className = ClassName(packageName, name)
  val builder = TypeSpec.objectBuilder(adapterName)

  if (generateAsInternal) {
    builder.addModifiers(KModifier.INTERNAL)
  }
  builder.addSuperinterface(ResponseAdapter::class.asClassName().parameterizedBy(className))

  builder.addFunction(notImplementedFromResponseFunSpec(className))
  builder.addFunction(FunSpec.builder(Identifier.toResponse)
      .addParameter(Identifier.writer, JsonWriter::class)
      .addParameter(Identifier.responseAdapterCache, ResponseAdapterCache::class.asTypeName())
      .addParameter(Identifier.value, className)
      .addModifiers(KModifier.OVERRIDE)
      .addCode(CodeBlock.Builder().apply {
        addStatement("writer.beginObject()")
        forEach {
          if (!it.isRequired) {
            beginControlFlow("if (value.%L is %T)", kotlinNameForVariable(it.name), Input.Present::class)
            addStatement("writer.name(%S)", it.schemaName)
            addStatement("%L.toResponse(writer, ${Identifier.responseAdapterCache}, value.%L.value)", adapterInitializer(it.type), kotlinNameForVariable(it.name))
            endControlFlow()
          } else {
            addStatement("writer.name(%S)", it.schemaName)
            addStatement("%L.toResponse(writer, ${Identifier.responseAdapterCache}, value.%L)", adapterInitializer(it.type), kotlinNameForVariable(it.name))
          }
        }
        addStatement("writer.endObject()")
      }.build())
      .build()
  )

  return builder.build()
}
