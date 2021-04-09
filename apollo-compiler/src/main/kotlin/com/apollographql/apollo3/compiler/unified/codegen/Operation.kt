package com.apollographql.apollo3.compiler.unified.codegen

import com.apollographql.apollo3.api.Mutation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.QueryDocumentMinifier
import com.apollographql.apollo3.api.Subscription
import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.backend.codegen.makeDataClass
import com.apollographql.apollo3.compiler.unified.CodegenLayout
import com.apollographql.apollo3.compiler.unified.IrOperation
import com.apollographql.apollo3.compiler.unified.IrOperationType
import com.apollographql.apollo3.compiler.unified.codegen.adapter.dataResponseAdapterTypeSpecs
import com.apollographql.apollo3.compiler.unified.codegen.adapter.inputAdapterTypeSpec
import com.apollographql.apollo3.compiler.unified.codegen.helpers.maybeAddDescription
import com.apollographql.apollo3.compiler.unified.codegen.helpers.toNamedType
import com.apollographql.apollo3.compiler.unified.codegen.helpers.toParameterSpec
import com.apollographql.apollo3.compiler.unified.codegen.helpers.typeSpecs
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName

fun IrOperation.apolloFileSpecs(
    layout: CodegenLayout,
    generateFilterNotNull: Boolean,
    operationId: String,
    generateResponseFields: Boolean,
    generateQueryDocument: Boolean,
): List<ApolloFileSpec> {
  val list = mutableListOf<ApolloFileSpec>()

  list.add(ApolloFileSpec(layout.operationPackageName(filePath), typeSpec(layout, operationId, generateFilterNotNull, generateQueryDocument)))
  if (variables.isNotEmpty()) {
    list.add(ApolloFileSpec(layout.operationAdapterPackageName(filePath), variablesAdapterTypeSpec(layout)))
  }
  list.add(ApolloFileSpec(layout.operationAdapterPackageName(filePath), responseAdapterTypeSpec(layout)))
  if (generateResponseFields) {
    list.add(ApolloFileSpec(layout.operationResponseFieldsPackageName(filePath), responseFieldsTypeSpec(layout)))
  }

  return list
}

private fun IrOperation.typeSpec(
    layout: CodegenLayout,
    operationId: String,
    generateFilterNotNull: Boolean,
    generateQueryDocument: Boolean,
): TypeSpec {
  return TypeSpec.classBuilder(layout.operationName(this))
      .addSuperinterface(superInterfaceType(layout))
      .maybeAddDescription(description)
      .makeDataClass(variables.map { it.toNamedType().toParameterSpec(layout) })
      .addFunction(operationIdFunSpec())
      .addFunction(queryDocumentFunSpec(generateQueryDocument))
      .addFunction(nameFunSpec())
      .addFunction(serializeVariablesFunSpec(layout))
      .addFunction(adapterFunSpec(layout))
      .addFunction(responseFieldsFunSpec(layout))
      .addTypes(dataTypeSpecs(layout))
      .addType(companionTypeSpec(operationId, generateQueryDocument))
      .build()
      .maybeAddFilterNotNull(generateFilterNotNull)
}

private fun IrOperation.responseFieldsFunSpec(layout: CodegenLayout): FunSpec {
  return responseFieldsFunSpec(layout.operationResponseFieldsClassName(this))
}

private fun IrOperation.variablesAdapterTypeSpec(layout: CodegenLayout): TypeSpec {
  return variables.map { it.toNamedType() }
      .inputAdapterTypeSpec(
          layout = layout,
          adapterName = layout.operationVariablesAdapterName(this),
          adaptedTypeName = layout.operationClassName(this)
      )
}

private fun IrOperation.responseAdapterTypeSpec(layout: CodegenLayout): TypeSpec {
  return TypeSpec.objectBuilder(layout.operationResponseAdapterWrapperName(this))
      .addTypes(dataResponseAdapterTypeSpecs(layout, dataField))
      .build()
}

private fun IrOperation.responseFieldsTypeSpec(layout: CodegenLayout): TypeSpec {
  return dataResponseFieldsItemSpec(layout.operationResponseFieldsName(this), dataField)
}

private fun IrOperation.serializeVariablesFunSpec(layout: CodegenLayout): FunSpec = serializeVariablesFunSpec(
    adapterPackageName = layout.operationAdapterPackageName(this.filePath),
    adapterName = layout.operationVariablesAdapterName(this),
    isEmpty = variables.isEmpty(),
    emptyMessage = "// This operation doesn't have variables"
)

private fun IrOperation.adapterFunSpec(layout: CodegenLayout): FunSpec {
  check(dataField.typeFieldSet != null) // data is always a compound type

  return adapterFunSpec(
      adapterTypeName = layout.operationResponseAdapterClassName(this),
      adaptedTypeName = layout.fieldSetClassName(dataField.typeFieldSet)
  )
}

private fun IrOperation.dataTypeSpecs(layout: CodegenLayout): List<TypeSpec> {
  val superClass = when (operationType) {
    IrOperationType.Query -> Query.Data::class
    IrOperationType.Mutation -> Mutation.Data::class
    IrOperationType.Subscription -> Subscription.Data::class
  }

  return dataField.typeSpecs(layout, false).map {
    it.toBuilder()
        .addSuperinterface(superClass)
        .build()
  }
}

private fun IrOperation.superInterfaceType(layout: CodegenLayout): TypeName {
  check(dataField.typeFieldSet != null) // data is always a compound type

  return when (operationType) {
    IrOperationType.Query -> Query::class.asTypeName()
    IrOperationType.Mutation -> Mutation::class.asTypeName()
    IrOperationType.Subscription -> Subscription::class.asTypeName()
  }.parameterizedBy(layout.fieldSetClassName(dataField.typeFieldSet))
}

private fun operationIdFunSpec() = FunSpec.builder("operationId")
    .addModifiers(KModifier.OVERRIDE)
    .returns(String::class)
    .addStatement("return OPERATION_ID")
    .build()

private fun queryDocumentFunSpec(generateQueryDocument: Boolean) = FunSpec.builder("queryDocument")
    .addModifiers(KModifier.OVERRIDE)
    .returns(String::class)
    .apply {
      if (generateQueryDocument) {
        addStatement("return QUERY_DOCUMENT")
      } else {
        addStatement("error(\"The query document was removed from this operation. Use generateQueryDocument = true if you need it\"")
      }
    }
    .build()

private fun nameFunSpec() = FunSpec.builder("name")
    .addModifiers(KModifier.OVERRIDE)
    .returns(String::class)
    .addStatement("return OPERATION_NAME")
    .build()

private fun IrOperation.companionTypeSpec(operationId: String, generateQueryDocument: Boolean): TypeSpec {
  return TypeSpec.companionObjectBuilder()
      .addProperty(PropertySpec.builder("OPERATION_ID", String::class)
          .addModifiers(KModifier.CONST)
          .initializer("%S", operationId)
          .build()
      )
      .applyIf(generateQueryDocument) {
        addProperty(PropertySpec.builder("QUERY_DOCUMENT", String::class)
            .initializer(
                CodeBlock.builder()
                    .add("%T.minify(\n", QueryDocumentMinifier::class.java)
                    .indent()
                    .add("%S\n", sourceWithFragments)
                    .unindent()
                    .add(")")
                    .build()
            )
            .build()
        )
      }
      .addProperty(PropertySpec
          .builder("OPERATION_NAME", String::class)
          .addModifiers(KModifier.CONST)
          .initializer("%S", name)
          .build()
      )
      .build()
}