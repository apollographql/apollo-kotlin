package com.apollographql.apollo.compiler.backend.codegen

import com.apollographql.apollo.api.CustomScalar
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.internal.ResponseWriter
import com.apollographql.apollo.api.internal.json.JsonWriter
import com.apollographql.apollo.compiler.applyIf
import com.apollographql.apollo.compiler.backend.ast.CodeGenerationAst
import com.apollographql.apollo.compiler.backend.ast.toLowerCamelCase
import com.apollographql.apollo.compiler.escapeKotlinReservedWord
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.joinToCode

internal fun CodeGenerationAst.ObjectType.writeToResponseFunSpec(): FunSpec {
  return when (this.kind) {
    is CodeGenerationAst.ObjectType.Kind.Fragment -> writeFragmentToResponseFunSpec()
    is CodeGenerationAst.ObjectType.Kind.FragmentDelegate -> writeFragmentDelegateToResponseFunSpec()
    else -> writeObjectToResponseFunSpec()
  }
}

private fun CodeGenerationAst.ObjectType.writeObjectToResponseFunSpec(): FunSpec {
  val writeFieldsCode = this.fields.mapIndexed { index, field ->
    field.writeCode()
  }.joinToCode(separator = "")
  return FunSpec.builder("toResponse")
      .applyIf(!isTypeCase) { addModifiers(KModifier.OVERRIDE) }
      .addParameter(ParameterSpec(name = "writer", type = JsonWriter::class.asTypeName()))
      .addParameter(ParameterSpec(name = "value", type = this.typeRef.asTypeName()))
      .addCode(writeFieldsCode)
      .build()
}

private fun CodeGenerationAst.ObjectType.writeFragmentDelegateToResponseFunSpec(): FunSpec {
  val fragmentRef = (this.kind as CodeGenerationAst.ObjectType.Kind.FragmentDelegate).fragmentTypeRef
  return FunSpec.builder("toResponse")
      .addModifiers(KModifier.OVERRIDE)
      .addParameter(ParameterSpec(name = "writer", type = ResponseWriter::class.asTypeName()))
      .addParameter(ParameterSpec(name = "value", type = this.typeRef.asTypeName()))
      .addStatement("%T.toResponse(writer,·value.delegate)", fragmentRef.enclosingType!!.asAdapterTypeName())
      .build()
}

private fun CodeGenerationAst.ObjectType.writeFragmentToResponseFunSpec(): FunSpec {
  val (defaultImplementation, possibleImplementations) = with(this.kind as CodeGenerationAst.ObjectType.Kind.Fragment) {
    defaultImplementation to possibleImplementations
  }
  return FunSpec.builder("toResponse")
      .addModifiers(KModifier.OVERRIDE)
      .addParameter(ParameterSpec(name = "writer", type = JsonWriter::class.asTypeName()))
      .addParameter(ParameterSpec(name = "value", type = this.typeRef.asTypeName()))
      .applyIf(possibleImplementations.isEmpty()) {
        addCode(
            this@writeFragmentToResponseFunSpec.fields.mapIndexed { index, field ->
              field.writeCode()
            }.joinToCode(separator = "")
        )
      }
      .applyIf(possibleImplementations.isNotEmpty()) {
        beginControlFlow("when(value)")
        addCode(
            possibleImplementations
                .values
                .distinct()
                .map { type ->
                  CodeBlock.of(
                      "is·%T·->·${type.name.toLowerCamelCase()}Adapter.toResponse(writer,·value)",
                      type.asTypeName(),
                  )
                }
                .joinToCode(separator = "\n", suffix = "\n")
        )
        addStatement(
            "is·%T·->·${defaultImplementation.name.toLowerCamelCase()}Adapter.toResponse(writer,·value)",
            defaultImplementation.asTypeName(),
        )
        endControlFlow()
      }
      .build()
}

private fun CodeGenerationAst.Field.writeCode(): CodeBlock {
  return CodeBlock.builder().apply {
    addStatement("${name.escapeKotlinReservedWord()}Adapter.toResponse(writer, value.${name.escapeKotlinReservedWord()})")
  }.build()
}
