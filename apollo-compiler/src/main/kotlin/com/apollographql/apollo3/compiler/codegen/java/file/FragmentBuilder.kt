package com.apollographql.apollo3.compiler.codegen.java.file

import com.apollographql.apollo3.compiler.codegen.java.CodegenJavaFile
import com.apollographql.apollo3.compiler.codegen.java.JavaClassBuilder
import com.apollographql.apollo3.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo3.compiler.codegen.java.JavaContext
import com.apollographql.apollo3.compiler.codegen.java.helpers.makeDataClassFromParameters
import com.apollographql.apollo3.compiler.codegen.java.helpers.maybeAddDescription
import com.apollographql.apollo3.compiler.codegen.java.helpers.toNamedType
import com.apollographql.apollo3.compiler.codegen.java.helpers.toParameterSpec
import com.apollographql.apollo3.compiler.codegen.java.model.ModelBuilder
import com.apollographql.apollo3.compiler.codegen.maybeFlatten
import com.apollographql.apollo3.compiler.ir.IrNamedFragment
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

class FragmentBuilder(
    private val context: JavaContext,
    private val fragment: IrNamedFragment,
    flatten: Boolean,
) : JavaClassBuilder {
  private val layout = context.layout
  private val packageName = layout.fragmentPackageName(fragment.filePath)
  private val simpleName = layout.fragmentName(fragment.name)

  private val modelBuilders = if (fragment.interfaceModelGroup != null) {
    fragment.dataModelGroup.maybeFlatten(
        flatten = flatten,
        excludeNames = setOf(simpleName)
    ).flatMap { it.models }.map {
      ModelBuilder(
          context = context,
          model = it,
          superClassName = if (it.id == fragment.dataModelGroup.baseModelId) JavaClassNames.FragmentData else null,
          path = listOf(packageName, simpleName)
      )
    }
  } else {
    // The data models are written outside the fragment
    emptyList()
  }

  override fun prepare() {
    context.resolver.registerFragment(
        fragment.name,
        ClassName.get(packageName, simpleName)
    )
    modelBuilders.forEach {
      it.prepare()
    }
  }

  override fun build(): CodegenJavaFile {
    return CodegenJavaFile(
        packageName = packageName,
        typeSpec = fragment.typeSpec()
    )
  }

  private fun IrNamedFragment.typeSpec(): TypeSpec {
    return TypeSpec.classBuilder(simpleName)
        .addModifiers(Modifier.PUBLIC)
        .addSuperinterface(superInterfaceType())
        .maybeAddDescription(description)
        .makeDataClassFromParameters(variables.map { it.toNamedType().toParameterSpec(context) })
        .addMethod(serializeVariablesMethodSpec())
        .addMethod(adapterMethodSpec())
        .addMethod(selectionsMethodSpec())
        // Fragments can have multiple data shapes
        .addTypes(dataTypeSpecs())
        .build()
  }

  private fun IrNamedFragment.selectionsMethodSpec(): MethodSpec {
    return rootFieldMethodSpec(
        context, fragment.typeCondition, context.resolver.resolveFragmentSelections(name)
    )
  }

  private fun IrNamedFragment.serializeVariablesMethodSpec(): MethodSpec = serializeVariablesMethodSpec(
      adapterClassName = context.resolver.resolveFragmentVariablesAdapter(name),
      emptyMessage = "This fragment doesn't have any variable",
  )

  private fun IrNamedFragment.adapterMethodSpec(): MethodSpec {
    return adapterMethodSpec(
        adapterTypeName = context.resolver.resolveModelAdapter(dataModelGroup.baseModelId),
        adaptedTypeName = context.resolver.resolveModel(dataModelGroup.baseModelId)
    )
  }

  private fun dataTypeSpecs(): List<TypeSpec> {
    return modelBuilders.map { it.build() }
  }

  private fun superInterfaceType(): TypeName {
    return ParameterizedTypeName.get(JavaClassNames.Fragment, context.resolver.resolveModel(fragment.dataModelGroup.baseModelId))
  }
}


