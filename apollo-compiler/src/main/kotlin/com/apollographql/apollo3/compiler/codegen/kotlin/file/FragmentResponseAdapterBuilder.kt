package com.apollographql.apollo3.compiler.codegen.kotlin.file

import com.apollographql.apollo3.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.codegen.kotlin.adapter.ResponseAdapterBuilder
import com.apollographql.apollo3.compiler.codegen.maybeFlatten
import com.apollographql.apollo3.compiler.ir.IrFragmentDefinition
import com.squareup.kotlinpoet.TypeSpec

internal class FragmentResponseAdapterBuilder(
    val context: KotlinContext,
    val fragment: IrFragmentDefinition,
    val flatten: Boolean,
) : CgFileBuilder {
  private val packageName = context.layout.fragmentPackageName(fragment.filePath)
  private val simpleName = context.layout.fragmentResponseAdapterWrapperName(fragment.name)

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
