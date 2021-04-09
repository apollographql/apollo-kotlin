package com.apollographql.apollo3.compiler.unified.codegen

import com.apollographql.apollo3.api.Mutation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.QueryDocumentMinifier
import com.apollographql.apollo3.api.Subscription
import com.apollographql.apollo3.compiler.backend.codegen.adapterPackageName
import com.apollographql.apollo3.compiler.backend.codegen.kotlinNameForOperation
import com.apollographql.apollo3.compiler.backend.codegen.kotlinNameForResponseAdapter
import com.apollographql.apollo3.compiler.backend.codegen.kotlinNameForResponseFields
import com.apollographql.apollo3.compiler.backend.codegen.kotlinNameForVariablesAdapter
import com.apollographql.apollo3.compiler.backend.codegen.makeDataClass
import com.apollographql.apollo3.compiler.backend.codegen.responseFieldsPackageName
import com.apollographql.apollo3.compiler.unified.IrOperation
import com.apollographql.apollo3.compiler.unified.IrOperationType
import com.apollographql.apollo3.compiler.unified.codegen.adapter.dataResponseAdapterTypeSpecs
import com.apollographql.apollo3.compiler.unified.codegen.adapter.inputAdapterTypeSpec
import com.apollographql.apollo3.compiler.unified.codegen.helpers.adapterTypeName
import com.apollographql.apollo3.compiler.unified.codegen.helpers.maybeAddDescription
import com.apollographql.apollo3.compiler.unified.codegen.helpers.toNamedType
import com.apollographql.apollo3.compiler.unified.codegen.helpers.toParameterSpec
import com.apollographql.apollo3.compiler.unified.codegen.helpers.typeName
import com.apollographql.apollo3.compiler.unified.codegen.helpers.typeSpecs
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName

fun IrOperation.qualifiedTypeSpecs(): List<ApolloFileSpec> {
  val list = mutableListOf<ApolloFileSpec>()

  list.add(ApolloFileSpec(packageName, typeSpec()))
  if (variables.isNotEmpty()) {
    list.add(ApolloFileSpec(adapterPackageName(packageName), variablesAdapterTypeSpec()))
  }
  list.add(ApolloFileSpec(adapterPackageName(packageName), responseAdapterTypeSpec()))
  list.add(ApolloFileSpec(responseFieldsPackageName(packageName), responseFieldsTypeSpec()))

  return list
}

private fun IrOperation.typeSpec(): TypeSpec {
  return TypeSpec.classBuilder(kotlinNameForOperation(name))
      .addSuperinterface(superInterfaceType())
      .maybeAddDescription(description)
      .makeDataClass(variables.map { it.toNamedType().toParameterSpec() })
      .addFunction(operationIdFunSpec())
      .addFunction(queryDocumentFunSpec())
      .addFunction(nameFunSpec())
      .addFunction(serializeVariablesFunSpec())
      .addFunction(adapterFunSpec())
      .addFunction(responseFieldsFunSpec())
      .addTypes(dataTypeSpecs())
      .addType(companionTypeSpec())
      .build()
}

private fun IrOperation.typeName() = ClassName(packageName, kotlinNameForOperation(name))

private fun IrOperation.responseFieldsFunSpec(): FunSpec {
  val typeName = ClassName(responseFieldsPackageName(packageName), kotlinNameForResponseFields(name))
  return responseFieldsFunSpec(typeName)
}

private fun IrOperation.variablesAdapterTypeSpec(): TypeSpec {
  return variables.map { it.toNamedType() }
      .inputAdapterTypeSpec(
          kotlinNameForVariablesAdapter(name),
          adaptedTypeName = typeName()
      )
}

private fun IrOperation.responseAdapterTypeSpec(): TypeSpec {
  return TypeSpec.objectBuilder(kotlinNameForResponseAdapter(name))
      .addTypes(dataResponseAdapterTypeSpecs(dataField))
      .build()
}

private fun IrOperation.responseFieldsTypeSpec(): TypeSpec {
  return dataResponseFieldsItemSpec(kotlinNameForResponseFields(name), dataField)
}

private fun IrOperation.serializeVariablesFunSpec(): FunSpec = serializeVariablesFunSpec(
    packageName = packageName,
    adapterName = kotlinNameForVariablesAdapter(name),
    isEmpty = variables.isEmpty(),
    emptyMessage = "// This operation doesn't have variables"
)

private fun IrOperation.adapterFunSpec(): FunSpec {
  check(dataField.typeFieldSet != null) // data is always a compound type

  return adapterFunSpec(
      adapterTypeName = dataField.typeFieldSet.adapterTypeName(),
      adaptedTypeName = dataField.typeFieldSet.typeName()
  )
}

private fun IrOperation.dataTypeSpecs(): List<TypeSpec> {
  val superClass = when (operationType) {
    IrOperationType.Query -> Query.Data::class
    IrOperationType.Mutation -> Mutation.Data::class
    IrOperationType.Subscription -> Subscription.Data::class
  }

  return dataField.typeSpecs(false).map {
    it.toBuilder()
        .addSuperinterface(superClass)
        .build()
  }
}

private fun IrOperation.superInterfaceType(): TypeName {
  check(dataField.typeFieldSet != null) // data is always a compound type

  return when (operationType) {
    IrOperationType.Query -> Query::class.asTypeName()
    IrOperationType.Mutation -> Mutation::class.asTypeName()
    IrOperationType.Subscription -> Subscription::class.asTypeName()
  }.parameterizedBy(dataField.typeFieldSet.typeName())
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

private fun IrOperation.companionTypeSpec(): TypeSpec {
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