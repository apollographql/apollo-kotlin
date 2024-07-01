package com.apollographql.apollo.compiler.codegen.kotlin.operations

import com.apollographql.apollo.compiler.codegen.fragmentResponseFieldsPackageName
import com.apollographql.apollo.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinOperationsContext
import com.apollographql.apollo.compiler.codegen.selections
import com.apollographql.apollo.compiler.ir.IrFragmentDefinition
import com.squareup.kotlinpoet.ClassName

internal class FragmentSelectionsBuilder(
    val context: KotlinOperationsContext,
    val fragment: IrFragmentDefinition,
) : CgFileBuilder {
  private val packageName = context.layout.fragmentResponseFieldsPackageName(fragment.filePath)
  private val simpleName = context.layout.fragmentName(fragment.name).selections()

  override fun prepare() {
    context.resolver.registerFragmentSelections(
        fragment.name,
        ClassName(packageName, simpleName)
    )
  }

  override fun build(): CgFile {
    return CgFile(
        packageName = packageName,
        fileName = simpleName,
        typeSpecs = listOf(
            CompiledSelectionsBuilder(
                context = context,
            ).build(fragment.selectionSets, simpleName)
        )
    )
  }
}