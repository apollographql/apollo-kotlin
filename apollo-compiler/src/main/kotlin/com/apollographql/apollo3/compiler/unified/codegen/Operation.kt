package com.apollographql.apollo3.compiler.unified.codegen

import com.apollographql.apollo3.api.Mutation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.QueryDocumentMinifier
import com.apollographql.apollo3.api.ResponseField
import com.apollographql.apollo3.api.Subscription
import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.backend.ast.CodeGenerationAst
import com.apollographql.apollo3.compiler.backend.codegen.adapterFunSpec
import com.apollographql.apollo3.compiler.backend.codegen.kotlinNameForOperation
import com.apollographql.apollo3.compiler.backend.codegen.makeDataClass
import com.apollographql.apollo3.compiler.backend.codegen.responseFieldsCode
import com.apollographql.apollo3.compiler.backend.codegen.superInterfaceType
import com.apollographql.apollo3.compiler.backend.codegen.suppressWarningsAnnotation
import com.apollographql.apollo3.compiler.backend.codegen.toParameterSpec
import com.apollographql.apollo3.compiler.backend.codegen.typeSpec
import com.apollographql.apollo3.compiler.escapeKotlinReservedWord
import com.apollographql.apollo3.compiler.unified.IrOperation
import com.apollographql.apollo3.compiler.unified.IrOperationType
import com.apollographql.apollo3.compiler.unified.codegen.helpers.toNamedType
import com.apollographql.apollo3.compiler.unified.codegen.helpers.toParameterSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName

fun IrOperation.typeSpec(): TypeSpec {
  return TypeSpec.classBuilder(kotlinNameForOperation(name))
      .addSuperinterface(superInterfaceType())
      .applyIf(description?.isNotBlank() == true) { addKdoc("%L", description!!) }
      .makeDataClass(variables.map { it.toNamedType().toParameterSpec() })
      .addFunction(operationIdFunSpec())
      .addFunction(queryDocumentFunSpec())
      .addFunction(nameFunSpec())
      .addFunction(serializeVariablesFunSpec(
          funName = "serializeVariables",
          packageName = targetPackage,
          name = name,
      ))
      .apply {
        val buffered = dataType.kind is CodeGenerationAst.ObjectType.Kind.ObjectWithFragments && !generateFragmentsAsInterfaces
        addFunction(adapterFunSpec(operationResponseAdapter, buffered))
      }
      .addFunction(
          FunSpec.builder(
              "responseFields",
          )
              .addModifiers(KModifier.OVERRIDE)
              .returns(
                  List::class.asClassName().parameterizedBy(
                      ResponseField.FieldSet::class.asClassName(),
                  )
              )
              .addCode("return %L", responseFieldsCode())
              .build()
      )
      .addType(this.dataType.typeSpec(generateFragmentsAsInterfaces))
      .addType(TypeSpec.companionObjectBuilder()
          .addProperty(PropertySpec.builder("OPERATION_ID", String::class)
              .addModifiers(KModifier.CONST)
              .initializer("%S", operationId)
              .build()
          )
          .addProperty(PropertySpec.builder("QUERY_DOCUMENT", String::class)
              .initializer(
                  CodeBlock.builder()
                      .add("%T.minify(\n", QueryDocumentMinifier::class.java)
                      .indent()
                      .add("%S\n", queryDocument)
                      .unindent()
                      .add(")")
                      .build()
              )
              .build()
          )
          .addProperty(PropertySpec
              .builder("OPERATION_NAME", String::class)
              .initializer("%S", operationName)
              .build()
          )
          .build()
      )
      .build()
}

private fun IrOperation.superInterfaceType(): TypeName {
  return when (operationType) {
    IrOperationType.Query -> Query::class.asClassName()
    IrOperationType.Mutation -> Mutation::class.asClassName()
    IrOperationType.Subscription -> Subscription::class.asClassName()
  }.parameterizedBy(dataField.baseFieldSet!!.fullPath.typeName())
}

private fun operationIdFunSpec() = FunSpec.builder("operationId")
    .addModifiers(KModifier.OVERRIDE)
    .returns(String::class)
    .addStatement("return OPERATION_ID")
    .build()

private fun queryDocumentFunSpec() = FunSpec.builder("queryDocument")
    .addModifiers(KModifier.OVERRIDE)
    .returns(String::class)
    .addStatement("return QUERY_DOCUMENT")
    .build()

private fun nameFunSpec() = FunSpec.builder("name")
    .addModifiers(KModifier.OVERRIDE)
    .returns(String::class)
    .addStatement("return OPERATION_NAME")
    .build()