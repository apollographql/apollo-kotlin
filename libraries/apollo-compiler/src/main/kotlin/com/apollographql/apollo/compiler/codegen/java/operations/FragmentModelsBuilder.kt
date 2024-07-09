package com.apollographql.apollo.compiler.codegen.java.operations

import com.apollographql.apollo.compiler.codegen.fragmentPackageName
import com.apollographql.apollo.compiler.codegen.java.CodegenJavaFile
import com.apollographql.apollo.compiler.codegen.java.JavaClassBuilder
import com.apollographql.apollo.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo.compiler.codegen.java.JavaOperationsContext
import com.apollographql.apollo.compiler.codegen.maybeFlatten
import com.apollographql.apollo.compiler.ir.IrFragmentDefinition
import com.apollographql.apollo.compiler.ir.IrModelGroup

internal class FragmentModelsBuilder(
    val context: JavaOperationsContext,
    val fragment: IrFragmentDefinition,
    modelGroup: IrModelGroup,
    private val addSuperInterface: Boolean,
    flatten: Boolean,
) : JavaClassBuilder {

  private val packageName = context.layout.fragmentPackageName(fragment.filePath)

  /**
   * Fragments need to be flattened at depth 1 to avoid having all classes polluting the fragments package name
   */
  private val modelBuilders = modelGroup.maybeFlatten(flatten, 1).flatMap { it.models }
      .map {
        ModelBuilder(
            context = context,
            model = it,
            superClassName = if (addSuperInterface && it.id == fragment.dataModelGroup.baseModelId) JavaClassNames.FragmentData else null,
            path = listOf(packageName)
        )
      }

  override fun prepare() {
    modelBuilders.forEach { it.prepare() }
  }

  override fun build(): CodegenJavaFile {
    return CodegenJavaFile(
        packageName = packageName,
        typeSpec = modelBuilders.map { it.build() }.single() // We know we will have only one top level class, see above
    )
  }
}
