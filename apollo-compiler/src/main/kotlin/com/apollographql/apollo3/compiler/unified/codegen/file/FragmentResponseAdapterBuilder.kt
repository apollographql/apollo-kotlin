package com.apollographql.apollo3.compiler.unified.codegen.file

import com.apollographql.apollo3.compiler.unified.codegen.CgContext
import com.apollographql.apollo3.compiler.unified.codegen.CgFile
import com.apollographql.apollo3.compiler.unified.codegen.CgFileBuilder
import com.apollographql.apollo3.compiler.unified.codegen.adapter.ResponseAdapterBuilder
import com.apollographql.apollo3.compiler.unified.ir.IrNamedFragment
import com.squareup.kotlinpoet.TypeSpec

class FragmentResponseAdapterBuilder(
    val context: CgContext,
    val fragment: IrNamedFragment,
) : CgFileBuilder {
  private val packageName = context.layout.fragmentPackageName(fragment.filePath)
  private val simpleName = context.layout.fragmentResponseAdapterWrapperName(fragment.name)

  private val responseAdapterBuilders = fragment.implementationModelGroups.map {
    ResponseAdapterBuilder.create(
        context = context,
        modelGroup = it,
        path = listOf(packageName, simpleName)
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

  private fun IrNamedFragment.responseAdapterTypeSpec(): TypeSpec {
    return TypeSpec.objectBuilder(simpleName)
        .addTypes(
            responseAdapterBuilders.flatMap { it.build() }
        )
        .build()
  }
}