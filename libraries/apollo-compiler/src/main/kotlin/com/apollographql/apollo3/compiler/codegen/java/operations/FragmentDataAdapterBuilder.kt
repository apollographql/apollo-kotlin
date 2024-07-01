package com.apollographql.apollo.compiler.codegen.java.operations

import com.apollographql.apollo.compiler.codegen.fragmentPackageName
import com.apollographql.apollo.compiler.codegen.impl
import com.apollographql.apollo.compiler.codegen.java.CodegenJavaFile
import com.apollographql.apollo.compiler.codegen.java.JavaClassBuilder
import com.apollographql.apollo.compiler.codegen.java.JavaOperationsContext
import com.apollographql.apollo.compiler.codegen.java.operations.util.ResponseAdapterBuilder
import com.apollographql.apollo.compiler.codegen.maybeFlatten
import com.apollographql.apollo.compiler.codegen.responseAdapter
import com.apollographql.apollo.compiler.ir.IrFragmentDefinition
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

internal class FragmentDataAdapterBuilder(
    val context: JavaOperationsContext,
    val fragment: IrFragmentDefinition,
    val flatten: Boolean,
) : JavaClassBuilder {
  private val packageName = context.layout.fragmentPackageName(fragment.filePath)
  private val simpleName = context.layout.fragmentName(fragment.name).impl().responseAdapter()

  private val responseAdapterBuilders = fragment.dataModelGroup.maybeFlatten(flatten).map {
    ResponseAdapterBuilder.create(
        context = context,
        modelGroup = it,
        path = listOf(packageName, simpleName),
        true
    )
  }

  override fun prepare() {
    responseAdapterBuilders.forEach { it.prepare() }
  }

  override fun build(): CodegenJavaFile {
    return CodegenJavaFile(
        packageName = packageName,
        typeSpec = fragment.responseAdapterTypeSpec()
    )
  }

  private fun IrFragmentDefinition.responseAdapterTypeSpec(): TypeSpec {
    return TypeSpec.classBuilder(simpleName)
        .addModifiers(Modifier.PUBLIC)
        .addTypes(
            responseAdapterBuilders.flatMap { it.build() }
        )
        .build()
  }
}
