package com.apollographql.apollo3.compiler.codegen.kotlin.file

import com.apollographql.apollo3.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo3.compiler.codegen.kotlin.model.ModelBuilder
import com.apollographql.apollo3.compiler.codegen.maybeFlatten
import com.apollographql.apollo3.compiler.ir.IrModelGroup
import com.apollographql.apollo3.compiler.ir.IrFragmentDefinition

internal class FragmentModelsBuilder(
    val context: KotlinContext,
    val fragment: IrFragmentDefinition,
    modelGroup: IrModelGroup,
    private val addSuperInterface: Boolean,
    flatten: Boolean,
) : CgFileBuilder {

  private val packageName = context.layout.fragmentPackageName(fragment.filePath)


  /**
   * For experimental_operationBasedWithInterfaces, fragments may have interfaces that are
   * only used locally. In that case, we can generate them as sealed interfaces
   */
  private val localInheritance = modelGroup.models.any { !it.isInterface }

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
            hasSubclassesInSamePackage = it.isInterface && localInheritance,
            adaptableWith = null
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
