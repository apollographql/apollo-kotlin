package com.apollographql.apollo3.compiler.backend.codegen

import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.Mutation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.ResponseField
import com.apollographql.apollo3.api.Subscription
import com.apollographql.apollo3.api.QueryDocumentMinifier
import com.apollographql.apollo3.api.ResponseAdapter
import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.backend.ast.CodeGenerationAst
import com.apollographql.apollo3.compiler.escapeKotlinReservedWord
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName

internal fun CodeGenerationAst.OperationType.typeSpec(
    targetPackage: String,
    generateAsInternal: Boolean,
    generateFragmentsAsInterfaces: Boolean,
): TypeSpec {
  val operationResponseAdapter = CodeGenerationAst.TypeRef(
      name = name,
      packageName = targetPackage,
      isNamedFragmentDataRef = false,
  ).asAdapterTypeName()

  return TypeSpec
      .classBuilder(kotlinNameForOperation(name))
      .addAnnotation(suppressWarningsAnnotation)
      .addSuperinterface(superInterfaceType(targetPackage))
      .applyIf(generateAsInternal) { addModifiers(KModifier.INTERNAL) }
      .applyIf(description.isNotBlank()) { addKdoc("%L", description) }
      .makeDataClass(variables.map { it.toParameterSpec() })
      .addFunction(FunSpec.builder("operationId")
          .addModifiers(KModifier.OVERRIDE)
          .returns(String::class)
          .addStatement("return OPERATION_ID")
          .build()
      )
      .addFunction(FunSpec.builder("queryDocument")
          .addModifiers(KModifier.OVERRIDE)
          .returns(String::class)
          .addStatement("return QUERY_DOCUMENT")
          .build()
      )
      .addFunction(serializeVariablesFunSpec(
          funName = "serializeVariables",
          packageName = targetPackage,
          name = name,
      ))
      .addFunction(FunSpec.builder("name")
          .addModifiers(KModifier.OVERRIDE)
          .returns(String::class)
          .addStatement("return OPERATION_NAME")
          .build()
      )
      .addFunction(adapterFunSpec(operationResponseAdapter))
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

private fun adapterFunSpec(operationResponseAdapter: ClassName): FunSpec {
  val body = CodeBlock.builder().apply {
    addStatement("val adapter = ${Identifier.RESPONSE_ADAPTER_CACHE}.getAdapterFor(this::class) {")
    indent()
    addStatement("%T(${Identifier.RESPONSE_ADAPTER_CACHE})", operationResponseAdapter)
    unindent()
    addStatement("}")
    addStatement("return adapter")
  }.build()

  return FunSpec.builder("adapter")
      .addModifiers(KModifier.OVERRIDE)
      .addParameter(ParameterSpec.builder(Identifier.RESPONSE_ADAPTER_CACHE, ResponseAdapterCache::class.asTypeName()).build())
      .returns(ResponseAdapter::class.asClassName().parameterizedBy(ClassName(packageName = "", "Data")))
      .addCode(body)
      .build()

}

private fun CodeGenerationAst.OperationType.superInterfaceType(targetPackage: String): TypeName {
  val dataTypeName = ClassName(targetPackage, name.escapeKotlinReservedWord(), "Data")
  return when (type) {
    CodeGenerationAst.OperationType.Type.QUERY -> Query::class.asClassName()
    CodeGenerationAst.OperationType.Type.MUTATION -> Mutation::class.asClassName()
    CodeGenerationAst.OperationType.Type.SUBSCRIPTION -> Subscription::class.asClassName()
  }.parameterizedBy(dataTypeName)
}

private fun CodeGenerationAst.OperationType.responseFieldsCode(): CodeBlock {
  val builder = CodeBlock.builder()

  builder.add("listOf(\n")
  builder.indent()
  when (val kind = dataType.kind) {
    is CodeGenerationAst.ObjectType.Kind.Object -> {
      builder.add("%T(null, %T.RESPONSE_FIELDS)\n", ResponseField.FieldSet::class, dataType.typeRef.asAdapterTypeName())
    }
    is CodeGenerationAst.ObjectType.Kind.ObjectWithFragments -> {
      kind.possibleImplementations.forEach { (possibleTypes, typeRef) ->
        possibleTypes.forEach { possibleType ->
          builder.add("%T(%S, %T.RESPONSE_FIELDS),\n", ResponseField.FieldSet::class, possibleType, typeRef.asAdapterTypeName())
        }
      }
      if (kind.defaultImplementation != null) {
        builder.add("%T(null, %T.RESPONSE_FIELDS),\n", ResponseField.FieldSet::class, kind.defaultImplementation.asAdapterTypeName())
      }
    }
  }
  builder.unindent()
  builder.add(")")

  return builder.build()
}
