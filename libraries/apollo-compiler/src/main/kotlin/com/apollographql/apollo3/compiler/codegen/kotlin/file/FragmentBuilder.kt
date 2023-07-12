package com.apollographql.apollo3.compiler.codegen.kotlin.file

import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.makeDataClass
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.maybeAddDescription
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.maybeAddJsExport
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.toNamedType
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.toParameterSpec
import com.apollographql.apollo3.compiler.codegen.kotlin.model.ModelBuilder
import com.apollographql.apollo3.compiler.codegen.maybeFlatten
import com.apollographql.apollo3.compiler.ir.IrFragmentDefinition
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec

internal class FragmentBuilder(
    private val context: KotlinContext,
    private val generateFilterNotNull: Boolean,
    private val fragment: IrFragmentDefinition,
    flatten: Boolean,
    private val addJvmOverloads: Boolean,
    private val generateDataBuilders: Boolean,
    private val generateCompiledField: Boolean,
) : CgFileBuilder {
  private val layout = context.layout
  private val packageName = layout.fragmentPackageName(fragment.filePath)
  private val simpleName = layout.fragmentName(fragment.name)

  private val modelBuilders = if (fragment.interfaceModelGroup != null) {
    fragment.dataModelGroup.maybeFlatten(flatten).flatMap { it.models }.map {
      ModelBuilder(
          context = context,
          model = it,
          superClassName = if (it.id == fragment.dataModelGroup.baseModelId) KotlinSymbols.FragmentData else null,
          path = listOf(packageName, simpleName),
          hasSubclassesInSamePackage = true,
          adaptableWith = null,
          reservedNames = setOf("Companion")
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

  private fun IrFragmentDefinition.typeSpec(): TypeSpec {
    return TypeSpec.classBuilder(simpleName)
        .addSuperinterface(superInterfaceType())
        .maybeAddDescription(description)
        .makeDataClass(variables.map { it.toNamedType().toParameterSpec(context) }, addJvmOverloads)
        .addFunction(serializeVariablesFunSpec())
        .addFunction(adapterFunSpec(context, dataProperty))
        .addFunction(rootFieldFunSpec())
        .addFunction(schemaFunSpec(context))
        // Fragments can have multiple data shapes
        .addTypes(dataTypeSpecs())
        .maybeAddJsExport(context)
        .applyIf(generateDataBuilders) {
          addType(
              TypeSpec.companionObjectBuilder()
                  .addFunction(
                      dataBuilderCtor(
                          context = context,
                          modelId = fragment.dataModelGroup.baseModelId,
                          selectionsClassName = context.resolver.resolveFragmentSelections(name),
                          typename = fragment.typeCondition,
                          builderFactoryParameterRequired = fragment.isTypeConditionAbstract
                      )
                  )
                  .build()
          )
        }
        .build()
        .maybeAddFilterNotNull(generateFilterNotNull)
  }

  private fun IrFragmentDefinition.rootFieldFunSpec(): FunSpec {
    return rootFieldFunSpec(
        context, fragment.typeCondition, if (generateCompiledField) context.resolver.resolveFragmentSelections(name) else null
    )
  }

  private fun IrFragmentDefinition.serializeVariablesFunSpec(): FunSpec = serializeVariablesFunSpec(
      adapterClassName = context.resolver.resolveFragmentVariablesAdapter(name),
      emptyMessage = "This fragment doesn't have any variable",
  )

  private fun dataTypeSpecs(): List<TypeSpec> {
    return modelBuilders.map { it.build() }
  }

  private fun superInterfaceType(): TypeName {
    return KotlinSymbols.Fragment.parameterizedBy(
        context.resolver.resolveModel(fragment.dataModelGroup.baseModelId)
    )
  }
}


