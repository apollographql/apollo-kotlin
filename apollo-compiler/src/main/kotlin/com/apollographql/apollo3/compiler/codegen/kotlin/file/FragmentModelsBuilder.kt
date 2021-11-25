package com.apollographql.apollo3.compiler.codegen.kotlin.file

import com.apollographql.apollo3.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo3.compiler.codegen.kotlin.CgOutputFileBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.codegen.kotlin.model.ModelBuilder
import com.apollographql.apollo3.compiler.codegen.maybeFlatten
import com.apollographql.apollo3.compiler.ir.IrModelGroup
import com.apollographql.apollo3.compiler.ir.IrNamedFragment

class FragmentModelsBuilder(
    val context: KotlinContext,
    val fragment: IrNamedFragment,
    modelGroup: IrModelGroup,
    private val addSuperInterface: Boolean,
    flatten: Boolean,
) : CgOutputFileBuilder {

  private val packageName = context.layout.fragmentPackageName(fragment.filePath)

  /**
   * Fragments need to be flattened at depth 1 to avoid having all classes poluting the fragments package name
   */
  private val modelBuilders = modelGroup.maybeFlatten(flatten, 1).flatMap { it.models }
      .map {
        ModelBuilder(
            context = context,
            model = it,
            superClassName = if (addSuperInterface && it.id == fragment.dataModelGroup.baseModelId) KotlinSymbols.FragmentData else null,
            path = listOf(packageName),
            hasSubclassesInSamePackage = false,
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