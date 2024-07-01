package com.apollographql.apollo.compiler.codegen.java.operations

import com.apollographql.apollo.compiler.codegen.fragmentPackageName
import com.apollographql.apollo.compiler.codegen.impl
import com.apollographql.apollo.compiler.codegen.java.CodegenJavaFile
import com.apollographql.apollo.compiler.codegen.java.JavaClassBuilder
import com.apollographql.apollo.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo.compiler.codegen.java.JavaOperationsContext
import com.apollographql.apollo.compiler.codegen.java.helpers.makeClassFromParameters
import com.apollographql.apollo.compiler.codegen.java.helpers.maybeAddDescription
import com.apollographql.apollo.compiler.codegen.java.helpers.toNamedType
import com.apollographql.apollo.compiler.codegen.java.helpers.toParameterSpec
import com.apollographql.apollo.compiler.codegen.maybeFlatten
import com.apollographql.apollo.compiler.ir.IrFragmentDefinition
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

internal class FragmentBuilder(
    private val context: JavaOperationsContext,
    private val fragment: IrFragmentDefinition,
    flatten: Boolean,
) : JavaClassBuilder {
  private val layout = context.layout
  private val packageName = layout.fragmentPackageName(fragment.filePath)
  private val simpleName = context.layout.fragmentName(fragment.name).impl()

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

  private fun IrFragmentDefinition.typeSpec(): TypeSpec {
    return TypeSpec.classBuilder(simpleName)
        .addModifiers(Modifier.PUBLIC)
        .addSuperinterface(superInterfaceType())
        .maybeAddDescription(description)
        .makeClassFromParameters(
            context.generateMethods,
            variables.map { it.toNamedType().toParameterSpec(context) },
            className = context.resolver.resolveFragment(fragment.name)
        )
        .addMethod(serializeVariablesMethodSpec())
        .addMethod(adapterMethodSpec(context.resolver, fragment.dataProperty))
        .addMethod(selectionsMethodSpec())
        // Fragments can have multiple data shapes
        .addTypes(dataTypeSpecs())
        .build()
  }

  private fun IrFragmentDefinition.selectionsMethodSpec(): MethodSpec {
    return rootFieldMethodSpec(
        context, fragment.typeCondition, context.resolver.resolveFragmentSelections(name)
    )
  }

  private fun IrFragmentDefinition.serializeVariablesMethodSpec(): MethodSpec = serializeVariablesMethodSpec(
      adapterClassName = context.resolver.resolveFragmentVariablesAdapter(name),
      emptyMessage = "This fragment doesn't have any variable",
  )

  private fun dataTypeSpecs(): List<TypeSpec> {
    return modelBuilders.map { it.build() }
  }

  private fun superInterfaceType(): TypeName {
    return ParameterizedTypeName.get(JavaClassNames.Fragment, context.resolver.resolveModel(fragment.dataModelGroup.baseModelId))
  }
}


