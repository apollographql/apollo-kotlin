package com.apollographql.apollo3.compiler.unified.codegen.file

import com.apollographql.apollo3.compiler.unified.codegen.CgContext
import com.apollographql.apollo3.compiler.unified.codegen.CgFile
import com.apollographql.apollo3.compiler.unified.codegen.CgFileBuilder
import com.apollographql.apollo3.compiler.unified.codegen.model.ModelBuilder
import com.apollographql.apollo3.compiler.unified.ir.IrNamedFragment

class FragmentInterfacesBuilder(
    val context: CgContext,
    val fragment: IrNamedFragment,
) : CgFileBuilder {

  private val packageName = context.layout.fragmentPackageName(fragment.filePath)

  private val modelBuilders = fragment.interfaceModelGroups.flatMap { it.models }
      .map {
        ModelBuilder(
            context = context,
            model = it,
            superClassName = null,
            path = listOf(packageName)
        )
      }

  override fun prepare() {
    modelBuilders.forEach { it.prepare() }
  }

  override fun build(): CgFile {
    return CgFile(
        packageName = packageName,
        fileName = context.layout.fragmentInterfaceFileName(fragment.name),
        typeSpecs = modelBuilders.map { it.build() }
    )
  }
}