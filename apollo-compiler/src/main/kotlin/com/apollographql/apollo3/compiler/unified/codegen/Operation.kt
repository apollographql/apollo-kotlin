package com.apollographql.apollo3.compiler.unified.codegen

import com.apollographql.apollo3.api.Mutation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.QueryDocumentMinifier
import com.apollographql.apollo3.api.Subscription
import com.apollographql.apollo3.compiler.backend.codegen.makeDataClass
import com.apollographql.apollo3.compiler.unified.ClassLayout
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

fun IrOperation.qualifiedTypeSpecs(layout: ClassLayout, generateFilterNotNull: Boolean, operationId: String): List<ApolloFileSpec> {
  val list = mutableListOf<ApolloFileSpec>()

  list.add(ApolloFileSpec(layout.operationPackageName(packageName), typeSpec(layout, operationId, generateFilterNotNull)))
  if (variables.isNotEmpty()) {
    list.add(ApolloFileSpec(layout.operationAdapterPackageName(packageName), variablesAdapterTypeSpec(layout)))
  }
  list.add(ApolloFileSpec(layout.operationAdapterPackageName(packageName), responseAdapterTypeSpec(layout)))
  list.add(ApolloFileSpec(layout.operationResponseFieldsPackageName(packageName), responseFieldsTypeSpec(layout)))

  return list
}

private fun IrOperation.typeSpec(layout: ClassLayout, operationId: String, generateFilterNotNull: Boolean): TypeSpec {
  return TypeSpec.classBuilder(layout.operationName(this))
      .addSuperinterface(superInterfaceType(layout))
      .maybeAddDescription(description)
      .makeDataClass(variables.map { it.toNamedType().toParameterSpec(layout) })
      .addFunction(operationIdFunSpec())
      .addFunction(queryDocumentFunSpec())
      .addFunction(nameFunSpec())
      .addFunction(serializeVariablesFunSpec(layout))
      .addFunction(adapterFunSpec(layout))
      .addFunction(responseFieldsFunSpec(layout))
      .addTypes(dataTypeSpecs(layout))
      .addType(companionTypeSpec(operationId))
      .build()
      .maybeAddFilterNotNull(generateFilterNotNull)
}

private fun IrOperation.responseFieldsFunSpec(layout: ClassLayout): FunSpec {
  return responseFieldsFunSpec(layout.operationResponseFieldsClassName(this))
}

private fun IrOperation.variablesAdapterTypeSpec(layout: ClassLayout): TypeSpec {
  return variables.map { it.toNamedType() }
      .inputAdapterTypeSpec(
          layout = layout,
          adapterName = layout.operationVariablesAdapterName(this),
          adaptedTypeName = layout.operationClassName(this)
      )
}

private fun IrOperation.responseAdapterTypeSpec(layout: ClassLayout): TypeSpec {
  return TypeSpec.objectBuilder(layout.operationResponseAdapterWrapperName(this))
      .addTypes(dataResponseAdapterTypeSpecs(layout, dataField))
      .build()
}

private fun IrOperation.responseFieldsTypeSpec(layout: ClassLayout): TypeSpec {
  return dataResponseFieldsItemSpec(layout.operationResponseFieldsName(this), dataField)
}

private fun IrOperation.serializeVariablesFunSpec(layout: ClassLayout): FunSpec = serializeVariablesFunSpec(
    adapterPackageName = layout.operationAdapterPackageName(this.packageName),
    adapterName = layout.operationVariablesAdapterName(this),
    isEmpty = variables.isEmpty(),
    emptyMessage = "// This operation doesn't have variables"
)

private fun IrOperation.adapterFunSpec(layout: ClassLayout): FunSpec {
  check(dataField.typeFieldSet != null) // data is always a compound type

  return adapterFunSpec(
      adapterTypeName = layout.operationResponseAdapterClassName(this),
      adaptedTypeName = layout.fieldSetClassName(dataField.typeFieldSet)
  )
}

private fun IrOperation.dataTypeSpecs(layout: ClassLayout): List<TypeSpec> {
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

private fun IrOperation.superInterfaceType(layout: ClassLayout): TypeName {
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

private fun IrOperation.companionTypeSpec(operationId: String): TypeSpec {
  return TypeSpec.companionObjectBuilder()
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
                  .add("%S\n", sourceWithFragments)
                  .unindent()
                  .add(")")
                  .build()
          )
          .build()
      )
      .addProperty(PropertySpec
          .builder("OPERATION_NAME", String::class)
          .addModifiers(KModifier.CONST)
          .initializer("%S", name)
          .build()
      )
      .build()
}