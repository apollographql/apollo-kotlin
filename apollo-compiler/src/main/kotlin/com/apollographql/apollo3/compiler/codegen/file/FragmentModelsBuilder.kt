package com.apollographql.apollo3.compiler.codegen.file

import com.apollographql.apollo3.api.Fragment
import com.apollographql.apollo3.compiler.codegen.CgContext
import com.apollographql.apollo3.compiler.codegen.CgFile
import com.apollographql.apollo3.compiler.codegen.CgFileBuilder
import com.apollographql.apollo3.compiler.codegen.maybeFlatten
import com.apollographql.apollo3.compiler.codegen.model.ModelBuilder
import com.apollographql.apollo3.compiler.ir.IrModelGroup
import com.apollographql.apollo3.compiler.ir.IrNamedFragment
import com.squareup.kotlinpoet.asClassName

class FragmentModelsBuilder(
    val context: CgContext,
    val fragment: IrNamedFragment,
    modelGroup: IrModelGroup,
    private val addSuperInterface: Boolean,
    private val flatten: Boolean
) : CgFileBuilder {

  private val packageName = context.layout.fragmentPackageName(fragment.filePath)

  /**
   * Fragments need to be flattened at depth 1 to avoid having all classes poluting the fragments package name
   */
  private val modelBuilders = modelGroup.maybeFlatten(flatten, 1).flatMap { it.models }
      .map {
        ModelBuilder(
            context = context,
            model = it,
            superClassName = if (addSuperInterface && it.id == fragment.dataModelGroup.baseModelId) Fragment.Data::class.asClassName() else null,
            path = listOf(packageName)
        )
      }

  override fun prepare() {
    modelBuilders.forEach { it.prepare() }
  }

  override fun build(): CgFile {
    return CgFile(
        packageName = packageName,
        fileName = context.layout.fragmentModelsFileName(fragment.name),
        typeSpecs = modelBuilders.map { it.build() }
    )
  }
}