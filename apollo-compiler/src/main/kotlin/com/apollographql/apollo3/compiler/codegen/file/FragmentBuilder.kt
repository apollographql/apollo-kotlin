package com.apollographql.apollo3.compiler.codegen.file

import com.apollographql.apollo3.api.Fragment
import com.apollographql.apollo3.compiler.codegen.helpers.makeDataClass
import com.apollographql.apollo3.compiler.codegen.CgContext
import com.apollographql.apollo3.compiler.codegen.CgFile
import com.apollographql.apollo3.compiler.codegen.CgFileBuilder
import com.apollographql.apollo3.compiler.codegen.helpers.maybeAddDescription
import com.apollographql.apollo3.compiler.codegen.helpers.toNamedType
import com.apollographql.apollo3.compiler.codegen.helpers.toParameterSpec
import com.apollographql.apollo3.compiler.codegen.model.ModelBuilder
import com.apollographql.apollo3.compiler.unified.ir.IrNamedFragment
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName

class FragmentBuilder(
    private val context: CgContext,
    private val generateFilterNotNull: Boolean,
    private val fragment: IrNamedFragment,
) : CgFileBuilder {
  private val layout = context.layout
  private val packageName = layout.fragmentPackageName(fragment.filePath)
  private val simpleName = layout.fragmentName(fragment.name)

  private val modelBuilders = fragment.implementationModelGroups.flatMap {
    it.models
  }.map {
    ModelBuilder(
        context = context,
        model = it,
        superClassName = if (it.id == fragment.implementationId) Fragment.Data::class.asClassName() else null,
        path = listOf(packageName, simpleName)
    )
  }

  override fun prepare() {
    context.resolver.registerFragment(
        fragment.name,
        ClassName(packageName, simpleName)
    )
    modelBuilders.forEach {
      it.prepare()
    }
  }

  override fun build(): CgFile {
    return CgFile(
        packageName = packageName,
        fileName = simpleName,
        typeSpecs = listOf(fragment.typeSpec())
    )
  }

  private fun IrNamedFragment.typeSpec(): TypeSpec {
    return TypeSpec.classBuilder(simpleName)
        .addSuperinterface(superInterfaceType())
        .maybeAddDescription(description)
        .makeDataClass(variables.map { it.toNamedType().toParameterSpec(context) })
        .addFunction(serializeVariablesFunSpec())
        .addFunction(adapterFunSpec())
        .addFunction(responseFieldsFunSpec())
        // Fragments can have multiple data shapes
        .addTypes(dataTypeSpecs())
        .build()
        .maybeAddFilterNotNull(generateFilterNotNull)
  }

  private fun IrNamedFragment.responseFieldsFunSpec(): FunSpec {
    return responseFieldsFunSpec(
        context.resolver.resolveFragmentResponseFields(name)
    )
  }

  private fun IrNamedFragment.serializeVariablesFunSpec(): FunSpec = serializeVariablesFunSpec(
      adapterClassName = context.resolver.resolveFragmentVariablesAdapter(name),
      emptyMessage = "This fragment doesn't have any variable",
  )

  private fun IrNamedFragment.adapterFunSpec(): FunSpec {
    return adapterFunSpec(
        adapterTypeName = context.resolver.resolveModelAdapter(implementationId),
        adaptedTypeName = context.resolver.resolveModel(implementationId)
    )
  }

  private fun dataTypeSpecs(): List<TypeSpec> {
    return modelBuilders.map { it.build() }
  }

  private fun superInterfaceType(): TypeName {
    return Fragment::class.asTypeName().parameterizedBy(
        context.resolver.resolveModel(fragment.implementationId)
    )
  }
}


