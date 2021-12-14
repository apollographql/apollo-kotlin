package com.apollographql.apollo3.compiler.codegen.java.file

import com.apollographql.apollo3.compiler.codegen.java.CodegenJavaFile
import com.apollographql.apollo3.compiler.codegen.java.JavaClassBuilder
import com.apollographql.apollo3.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo3.compiler.codegen.java.JavaContext
import com.apollographql.apollo3.compiler.codegen.java.model.ModelBuilder
import com.apollographql.apollo3.compiler.codegen.maybeFlatten
import com.apollographql.apollo3.compiler.ir.IrModelGroup
import com.apollographql.apollo3.compiler.ir.IrNamedFragment

class FragmentModelsBuilder(
    val context: JavaContext,
    val fragment: IrNamedFragment,
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