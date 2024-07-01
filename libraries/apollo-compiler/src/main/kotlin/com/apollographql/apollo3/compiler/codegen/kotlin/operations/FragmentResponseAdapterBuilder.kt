package com.apollographql.apollo.compiler.codegen.kotlin.operations

import com.apollographql.apollo.compiler.codegen.fragmentPackageName
import com.apollographql.apollo.compiler.codegen.impl
import com.apollographql.apollo.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinOperationsContext
import com.apollographql.apollo.compiler.codegen.kotlin.operations.util.ResponseAdapterBuilder
import com.apollographql.apollo.compiler.codegen.maybeFlatten
import com.apollographql.apollo.compiler.codegen.responseAdapter
import com.apollographql.apollo.compiler.ir.IrFragmentDefinition
import com.squareup.kotlinpoet.TypeSpec

internal class FragmentResponseAdapterBuilder(
    val context: KotlinOperationsContext,
    val fragment: IrFragmentDefinition,
    val flatten: Boolean,
) : CgFileBuilder {
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

  override fun build(): CgFile {
    return CgFile(
        packageName = packageName,
        fileName = simpleName,
        typeSpecs = listOf(fragment.responseAdapterTypeSpec())
    )
  }

  private fun IrFragmentDefinition.responseAdapterTypeSpec(): TypeSpec {
    return TypeSpec.objectBuilder(simpleName)
        .addTypes(
            responseAdapterBuilders.flatMap { it.build() }
        )
        .build()
  }
}