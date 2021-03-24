package com.apollographql.apollo3.compiler.unified.codegen

import com.apollographql.apollo3.api.Fragment
import com.apollographql.apollo3.compiler.backend.codegen.adapterPackageName
import com.apollographql.apollo3.compiler.backend.codegen.kotlinNameForFragment
import com.apollographql.apollo3.compiler.backend.codegen.kotlinNameForResponseAdapter
import com.apollographql.apollo3.compiler.backend.codegen.kotlinNameForResponseFields
import com.apollographql.apollo3.compiler.backend.codegen.kotlinNameForVariablesAdapter
import com.apollographql.apollo3.compiler.backend.codegen.makeDataClass
import com.apollographql.apollo3.compiler.backend.codegen.responseFieldsPackageName
import com.apollographql.apollo3.compiler.unified.IrNamedFragment
import com.apollographql.apollo3.compiler.unified.baseFieldSet
import com.apollographql.apollo3.compiler.unified.codegen.adapter.dataResponseAdapterTypeSpecs
import com.apollographql.apollo3.compiler.unified.codegen.adapter.inputAdapterTypeSpec
import com.apollographql.apollo3.compiler.unified.codegen.helpers.adapterTypeName
import com.apollographql.apollo3.compiler.unified.codegen.helpers.rawTypeName
import com.apollographql.apollo3.compiler.unified.codegen.helpers.maybeAddDescription
import com.apollographql.apollo3.compiler.unified.codegen.helpers.toNamedType
import com.apollographql.apollo3.compiler.unified.codegen.helpers.toParameterSpec
import com.apollographql.apollo3.compiler.unified.codegen.helpers.typeName
import com.apollographql.apollo3.compiler.unified.codegen.helpers.typeSpecs
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName

fun IrNamedFragment.qualifiedTypeSpecs(): List<QualifiedTypeSpec> {
  val list = mutableListOf<QualifiedTypeSpec>()

  list.add(QualifiedTypeSpec(packageName, typeSpec()))
  if (variables.isNotEmpty()){
    list.add(QualifiedTypeSpec(adapterPackageName(packageName), variablesAdapterTypeSpec()))
  }
  list.add(QualifiedTypeSpec(adapterPackageName(packageName), responseAdapterTypeSpec()))
  list.add(QualifiedTypeSpec(responseFieldsPackageName(packageName), responseFieldsTypeSpec()))

  return list
}

private fun IrNamedFragment.typeSpec(): TypeSpec {
  return TypeSpec.classBuilder(kotlinNameForFragment(name))
      .addSuperinterface(superInterfaceType())
      .maybeAddDescription(description)
      .makeDataClass(variables.map { it.toNamedType().toParameterSpec() })
      .addFunction(serializeVariablesFunSpec())
      .addFunction(adapterFunSpec())
      .addFunction(responseFieldsFunSpec())
      .addTypes(dataTypeSpecs()) // Fragments can have multiple data shapes
      .build()
}

private fun IrNamedFragment.typeName() = ClassName(packageName, kotlinNameForFragment(name))

private fun IrNamedFragment.responseFieldsFunSpec(): FunSpec {
  val typeName = ClassName(responseFieldsPackageName(packageName), kotlinNameForResponseFields(name))
  return responseFieldsFunSpec(typeName)
}

private fun IrNamedFragment.variablesAdapterTypeSpec(): TypeSpec {
  return variables.map { it.toNamedType() }
      .inputAdapterTypeSpec(
          kotlinNameForVariablesAdapter(name),
          adaptedTypeName = typeName()
      )
}

private fun IrNamedFragment.responseAdapterTypeSpec(): TypeSpec {
  return TypeSpec.objectBuilder(kotlinNameForResponseAdapter(name))
      .addTypes(dataResponseAdapterTypeSpecs(dataField))
      .build()
}

private fun IrNamedFragment.responseFieldsTypeSpec(): TypeSpec {
  return dataResponseFieldsItemSpec(kotlinNameForResponseFields(name), dataField)
}

private fun IrNamedFragment.serializeVariablesFunSpec(): FunSpec = serializeVariablesFunSpec(
    packageName = packageName,
    name = name,
    isEmpty = variables.isEmpty()
)

private fun IrNamedFragment.adapterFunSpec(): FunSpec {
  return adapterFunSpec(
      adapterTypeName = dataField.implementations.baseFieldSet().adapterTypeName(),
      adaptedTypeName = dataField.rawTypeName()
  )
}

private fun IrNamedFragment.dataTypeSpecs(): List<TypeSpec> {
  return dataField.typeSpecs().map {
    it.toBuilder()
        .addSuperinterface(Fragment.Data::class)
        .build()
  }
}

private fun IrNamedFragment.superInterfaceType(): TypeName {
  return Fragment::class.asTypeName().parameterizedBy(dataField.baseFieldSet!!.typeName())
}
