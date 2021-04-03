package com.apollographql.apollo3.compiler.unified.codegen

import com.apollographql.apollo3.api.Fragment
import com.apollographql.apollo3.compiler.backend.codegen.makeDataClass
import com.apollographql.apollo3.compiler.unified.CodegenLayout
import com.apollographql.apollo3.compiler.unified.IrNamedFragment
import com.apollographql.apollo3.compiler.unified.codegen.adapter.dataResponseAdapterTypeSpecs
import com.apollographql.apollo3.compiler.unified.codegen.adapter.inputAdapterTypeSpec
import com.apollographql.apollo3.compiler.unified.codegen.helpers.maybeAddDescription
import com.apollographql.apollo3.compiler.unified.codegen.helpers.toNamedType
import com.apollographql.apollo3.compiler.unified.codegen.helpers.toParameterSpec
import com.apollographql.apollo3.compiler.unified.codegen.helpers.typeSpecs
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName

fun IrNamedFragment.apolloFileSpecs(
    layout: CodegenLayout,
    generateFilterNotNull: Boolean,
    generateFragmentImplementations: Boolean,
    generateResponseFields: Boolean,
): List<ApolloFileSpec> {
  val list = mutableListOf<ApolloFileSpec>()

  list.add(
      ApolloFileSpec(
          packageName = layout.fragmentPackageName(name),
          interfaceTypeSpecs(layout),
          layout.fragmentInterfaceFileName(name)
      )
  )
  if (generateFragmentImplementations) {
    list.add(ApolloFileSpec(layout.fragmentPackageName(name), implementationTypeSpec(layout, generateFilterNotNull)))
    list.add(ApolloFileSpec(layout.fragmentAdapterPackageName(name), responseAdapterTypeSpec(layout)))
    if (variables.isNotEmpty()) {
      list.add(ApolloFileSpec(layout.fragmentAdapterPackageName(name), variablesAdapterTypeSpec(layout)))
    }
    if (generateResponseFields) {
      list.add(ApolloFileSpec(layout.fragmentResponseFieldsPackageName(name), responseFieldsTypeSpec(layout)))
    }
  }

  return list
}

private fun IrNamedFragment.implementationTypeSpec(layout: CodegenLayout, generateFilterNotNull: Boolean): TypeSpec {
  return TypeSpec.classBuilder(layout.fragmentName(name))
      .addSuperinterface(superInterfaceType(layout))
      .maybeAddDescription(description)
      .makeDataClass(variables.map { it.toNamedType().toParameterSpec(layout) })
      .addFunction(serializeVariablesFunSpec(layout))
      .addFunction(adapterFunSpec(layout))
      .addFunction(responseFieldsFunSpec(layout))
      .addTypes(dataTypeSpecs(layout)) // Fragments can have multiple data shapes
      .build()
      .maybeAddFilterNotNull(generateFilterNotNull)
}

private fun IrNamedFragment.interfaceTypeSpecs(layout: CodegenLayout): List<TypeSpec> {
  return interfaceField.typeSpecs(layout, true)
}


private fun IrNamedFragment.responseFieldsFunSpec(layout: CodegenLayout): FunSpec {
  return responseFieldsFunSpec(layout.fragmentResponseFieldsClassName(name))
}

private fun IrNamedFragment.variablesAdapterTypeSpec(layout: CodegenLayout): TypeSpec {
  return variables.map { it.toNamedType() }
      .inputAdapterTypeSpec(
          layout = layout,
          adapterName = layout.fragmentVariablesAdapterName(name),
          adaptedTypeName = layout.fragmentImplementationClassName(name)
      )
}

private fun IrNamedFragment.responseAdapterTypeSpec(layout: CodegenLayout): TypeSpec {
  return TypeSpec.objectBuilder(layout.fragmentResponseAdapterWrapperName(name))
      .addTypes(dataResponseAdapterTypeSpecs(layout, implementationField))
      .build()
}

private fun IrNamedFragment.responseFieldsTypeSpec(layout:CodegenLayout): TypeSpec {
  return dataResponseFieldsItemSpec(layout.fragmentResponseFieldsName(name), implementationField)
}

private fun IrNamedFragment.serializeVariablesFunSpec(layout: CodegenLayout): FunSpec = serializeVariablesFunSpec(
    adapterPackageName = layout.fragmentAdapterPackageName(name),
    adapterName = layout.fragmentVariablesAdapterName(name),
    isEmpty = variables.isEmpty(),
    emptyMessage = "// This fragment doesn't have variables",
)

private fun IrNamedFragment.adapterFunSpec(layout: CodegenLayout): FunSpec {
  check(implementationField.typeFieldSet != null) // data is always a compound type

  return adapterFunSpec(
      adapterTypeName = layout.fieldSetAdapterClassName(implementationField.typeFieldSet),
      adaptedTypeName = layout.fieldSetClassName(implementationField.typeFieldSet)
  )
}

private fun IrNamedFragment.dataTypeSpecs(layout: CodegenLayout): List<TypeSpec> {
  return implementationField.typeSpecs(layout, false).map {
    it.toBuilder()
        .addSuperinterface(Fragment.Data::class)
        .build()
  }
}

private fun IrNamedFragment.superInterfaceType(layout: CodegenLayout): TypeName {
  check(implementationField.typeFieldSet != null) // data is always a compound type

  return Fragment::class.asTypeName().parameterizedBy(
      layout.fieldSetClassName(implementationField.typeFieldSet)
  )
}
