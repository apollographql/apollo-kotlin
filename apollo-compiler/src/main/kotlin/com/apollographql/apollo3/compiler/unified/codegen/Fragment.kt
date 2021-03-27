package com.apollographql.apollo3.compiler.unified.codegen

import com.apollographql.apollo3.api.Fragment
import com.apollographql.apollo3.compiler.backend.codegen.adapterPackageName
import com.apollographql.apollo3.compiler.backend.codegen.kotlinNameForFragmentInterfaceFile
import com.apollographql.apollo3.compiler.backend.codegen.kotlinNameForFragmentImplementation
import com.apollographql.apollo3.compiler.backend.codegen.kotlinNameForFragmentResponseAdapter
import com.apollographql.apollo3.compiler.backend.codegen.kotlinNameForFragmentVariablesAdapter
import com.apollographql.apollo3.compiler.backend.codegen.kotlinNameForResponseFields
import com.apollographql.apollo3.compiler.backend.codegen.makeDataClass
import com.apollographql.apollo3.compiler.backend.codegen.responseFieldsPackageName
import com.apollographql.apollo3.compiler.unified.IrNamedFragment
import com.apollographql.apollo3.compiler.unified.codegen.adapter.dataResponseAdapterTypeSpecs
import com.apollographql.apollo3.compiler.unified.codegen.adapter.inputAdapterTypeSpec
import com.apollographql.apollo3.compiler.unified.codegen.helpers.adapterTypeName
import com.apollographql.apollo3.compiler.unified.codegen.helpers.maybeAddDescription
import com.apollographql.apollo3.compiler.unified.codegen.helpers.toNamedType
import com.apollographql.apollo3.compiler.unified.codegen.helpers.toParameterSpec
import com.apollographql.apollo3.compiler.unified.codegen.helpers.typeName
import com.apollographql.apollo3.compiler.unified.codegen.helpers.typeSpec
import com.apollographql.apollo3.compiler.unified.codegen.helpers.typeSpecs
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName

fun IrNamedFragment.qualifiedTypeSpecs(): List<ApolloFileSpec> {
  val list = mutableListOf<ApolloFileSpec>()

  list.add(ApolloFileSpec(packageName, interfaceTypeSpecs(), kotlinNameForFragmentInterfaceFile(name)))
  list.add(ApolloFileSpec(packageName, implementationTypeSpec()))
  if (variables.isNotEmpty()){
    list.add(ApolloFileSpec(adapterPackageName(packageName), variablesAdapterTypeSpec()))
  }
  list.add(ApolloFileSpec(adapterPackageName(packageName), responseAdapterTypeSpec()))
  list.add(ApolloFileSpec(responseFieldsPackageName(packageName), responseFieldsTypeSpec()))

  return list
}

private fun IrNamedFragment.implementationTypeSpec(): TypeSpec {
  return TypeSpec.classBuilder(kotlinNameForFragmentImplementation(name))
      .addSuperinterface(superInterfaceType())
      .maybeAddDescription(description)
      .makeDataClass(variables.map { it.toNamedType().toParameterSpec() })
      .addFunction(serializeVariablesFunSpec())
      .addFunction(adapterFunSpec())
      .addFunction(responseFieldsFunSpec())
      .addTypes(dataTypeSpecs()) // Fragments can have multiple data shapes
      .build()
}

private fun IrNamedFragment.interfaceTypeSpecs(): List<TypeSpec> {
  return interfaceField.fieldSets.map {
    it.typeSpec(true)
  }
}

private fun IrNamedFragment.typeName() = ClassName(packageName, kotlinNameForFragmentImplementation(name))

private fun IrNamedFragment.responseFieldsFunSpec(): FunSpec {
  val typeName = ClassName(responseFieldsPackageName(packageName), kotlinNameForResponseFields(name))
  return responseFieldsFunSpec(typeName)
}

private fun IrNamedFragment.variablesAdapterTypeSpec(): TypeSpec {
  return variables.map { it.toNamedType() }
      .inputAdapterTypeSpec(
          kotlinNameForFragmentVariablesAdapter(name),
          adaptedTypeName = typeName()
      )
}

private fun IrNamedFragment.responseAdapterTypeSpec(): TypeSpec {
  return TypeSpec.objectBuilder(kotlinNameForFragmentResponseAdapter(name))
      .addTypes(dataResponseAdapterTypeSpecs(dataField))
      .build()
}

private fun IrNamedFragment.responseFieldsTypeSpec(): TypeSpec {
  return dataResponseFieldsItemSpec(kotlinNameForResponseFields(name), dataField)
}

private fun IrNamedFragment.serializeVariablesFunSpec(): FunSpec = serializeVariablesFunSpec(
    packageName = packageName,
    adapterName = kotlinNameForFragmentVariablesAdapter(name),
    isEmpty = variables.isEmpty(),
    emptyMessage = "// This fragment doesn't have variables",
)

private fun IrNamedFragment.adapterFunSpec(): FunSpec {
  check(dataField.typeFieldSet != null) // data is always a compound type

  return adapterFunSpec(
      adapterTypeName = dataField.typeFieldSet.adapterTypeName(),
      adaptedTypeName = dataField.typeFieldSet.typeName()
  )
}

private fun IrNamedFragment.dataTypeSpecs(): List<TypeSpec> {
  return dataField.typeSpecs(false).map {
    it.toBuilder()
        .addSuperinterface(Fragment.Data::class)
        .build()
  }
}

private fun IrNamedFragment.superInterfaceType(): TypeName {
  check(dataField.typeFieldSet != null) // data is always a compound type

  return Fragment::class.asTypeName().parameterizedBy(
      dataField.typeFieldSet.typeName()
  )
}
