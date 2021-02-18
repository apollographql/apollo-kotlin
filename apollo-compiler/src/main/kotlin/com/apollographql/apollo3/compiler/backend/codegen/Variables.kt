package com.apollographql.apollo3.compiler.backend.codegen

import com.apollographql.apollo3.api.Input
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.internal.InputFieldMarshaller
import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.backend.ast.CodeGenerationAst
import com.apollographql.apollo3.compiler.escapeKotlinReservedWord
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.joinToCode

private fun List<CodeGenerationAst.InputField>.constructorFunSpec() = FunSpec
    .constructorBuilder()
    .addParameters(map { variable ->
      ParameterSpec
          .builder(
              name = variable.name.escapeKotlinReservedWord(),
              type = variable.type.asTypeName().let { type ->
                if (type.isNullable) Input::class.asClassName().parameterizedBy(type.copy(nullable = false)) else type
              }
          )
          .applyIf(variable.type.nullable) { defaultValue("%T.absent()", Input::class.asClassName()) }
          .build()
    })
    .build()

private fun List<CodeGenerationAst.InputField>.variablePropertySpec(enclosingClassName: String) = PropertySpec
    .builder("variables", Operation.Variables::class)
    .addModifiers(KModifier.PRIVATE)
    .addAnnotation(Transient::class)
    .initializer("%L", TypeSpec.anonymousClassBuilder()
        .superclass(Operation.Variables::class)
        .addFunction(variablesValueMapSpec(enclosingClassName))
        .addFunction(variablesMarshallerSpec(enclosingClassName.escapeKotlinReservedWord()))
        .build()
    )
    .build()


private fun List<CodeGenerationAst.InputField>.variablesValueMapSpec(enclosingClassName: String): FunSpec {
  return FunSpec
      .builder("valueMap")
      .addModifiers(KModifier.OVERRIDE)
      .returns(Map::class.asClassName().parameterizedBy(String::class.asClassName(), Any::class.asClassName().copy(nullable = true)))
      .beginControlFlow("return mutableMapOf<%T, %T>().apply", String::class, Any::class.asClassName().copy(nullable = true))
      .addCode(
          map { field ->
            if (field.type.nullable) {
              CodeBlock.builder()
                  .addStatement(
                      "if·(this@%L.%L.isPresent)·{",
                      enclosingClassName.escapeKotlinReservedWord(),
                      field.name.escapeKotlinReservedWord()
                  )
                  .indent()
                  .addStatement(
                      "this[%S]·=·this@%L.%L.getOrThrow()",
                      field.schemaName,
                      enclosingClassName.escapeKotlinReservedWord(),
                      field.name.escapeKotlinReservedWord()
                  )
                  .unindent()
                  .addStatement("}")
                  .build()
            } else {
              CodeBlock.of(
                  "this[%S]·=·this@%L.%L\n",
                  field.schemaName,
                  enclosingClassName.escapeKotlinReservedWord(),
                  field.name.escapeKotlinReservedWord()
              )
            }
          }.joinToCode(separator = "")
      )
      .endControlFlow()
      .build()
}

private fun List<CodeGenerationAst.InputField>.variablesMarshallerSpec(thisRef: String): FunSpec {
  return FunSpec
      .builder("marshaller")
      .returns(InputFieldMarshaller::class)
      .addModifiers(KModifier.OVERRIDE)
      .addCode(CodeBlock
          .builder()
          .beginControlFlow("return·%T.invoke·{ writer ->", InputFieldMarshaller::class)
          .apply { forEach { field -> add(field.writeCodeBlock(thisRef)) } }
          .endControlFlow()
          .build()
      )
      .build()
}

internal fun List<CodeGenerationAst.InputField>.variablesFunSpec() = FunSpec.builder("variables")
    .addModifiers(KModifier.OVERRIDE)
    .returns(Operation.Variables::class.asClassName())
    .apply {
      if (isNotEmpty()) {
        addStatement("return variables")
      } else {
        addStatement("return %T.EMPTY_VARIABLES", Operation::class)
      }
    }
    .build()

internal fun TypeSpec.Builder.addVariablesIfNeeded(variables: List<CodeGenerationAst.InputField>, enclosingClassName: String) = applyIf(variables.isNotEmpty()) {
  addModifiers(KModifier.DATA)
  primaryConstructor(variables.constructorFunSpec())
  addProperties(variables.map { variable -> variable.asPropertySpec(CodeBlock.of(variable.name.escapeKotlinReservedWord())) })
  addProperty(variables.variablePropertySpec(enclosingClassName))
}
