package com.apollographql.apollo3.compiler.codegen.kotlin.file

import com.apollographql.apollo3.api.Fragment
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.makeDataClass
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.CgOutputFileBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.maybeAddDescription
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.toNamedType
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.toParameterSpec
import com.apollographql.apollo3.compiler.codegen.maybeFlatten
import com.apollographql.apollo3.compiler.codegen.kotlin.model.ModelBuilder
import com.apollographql.apollo3.compiler.ir.IrNamedFragment
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName

class FragmentBuilder(
    private val context: KotlinContext,
    private val generateFilterNotNull: Boolean,
    private val fragment: IrNamedFragment,
    flatten: Boolean,
    flattenNamesInOrder: Boolean,
) : CgOutputFileBuilder {
  private val layout = context.layout
  private val packageName = layout.fragmentPackageName(fragment.filePath)
  private val simpleName = layout.fragmentName(fragment.name)

  private val modelBuilders = if (fragment.interfaceModelGroup != null) {
    fragment.dataModelGroup.maybeFlatten(flatten, flattenNamesInOrder).flatMap { it.models }.map {
      ModelBuilder(
          context = context,
          model = it,
          superClassName = if (it.id == fragment.dataModelGroup.baseModelId) Fragment.Data::class.asClassName() else null,
          path = listOf(packageName, simpleName),
          hasSubclassesInSamePackage = false,
      )
    }
  } else {
    // The data models are written outside the fragment
    emptyList()
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
        .addFunction(selectionsFunSpec())
        // Fragments can have multiple data shapes
        .addTypes(dataTypeSpecs())
        .build()
        .maybeAddFilterNotNull(generateFilterNotNull)
  }

  private fun IrNamedFragment.selectionsFunSpec(): FunSpec {
    return selectionsFunSpec(
        context, context.resolver.resolveFragmentSelections(name)
    )
  }

  private fun IrNamedFragment.serializeVariablesFunSpec(): FunSpec = serializeVariablesFunSpec(
      adapterClassName = context.resolver.resolveFragmentVariablesAdapter(name),
      emptyMessage = "This fragment doesn't have any variable",
  )

  private fun IrNamedFragment.adapterFunSpec(): FunSpec {
    return adapterFunSpec(
        adapterTypeName = context.resolver.resolveModelAdapter(dataModelGroup.baseModelId),
        adaptedTypeName = context.resolver.resolveModel(dataModelGroup.baseModelId)
    )
  }

  private fun dataTypeSpecs(): List<TypeSpec> {
    return modelBuilders.map { it.build() }
  }

  private fun superInterfaceType(): TypeName {
    return Fragment::class.asTypeName().parameterizedBy(
        context.resolver.resolveModel(fragment.dataModelGroup.baseModelId)
    )
  }
}


