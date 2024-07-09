package com.apollographql.apollo.compiler.codegen.java.operations

import com.apollographql.apollo.compiler.codegen.fragmentResponseFieldsPackageName
import com.apollographql.apollo.compiler.codegen.java.CodegenJavaFile
import com.apollographql.apollo.compiler.codegen.java.JavaClassBuilder
import com.apollographql.apollo.compiler.codegen.java.JavaOperationsContext
import com.apollographql.apollo.compiler.codegen.selections
import com.apollographql.apollo.compiler.ir.IrFragmentDefinition
import com.squareup.javapoet.ClassName

internal class FragmentSelectionsBuilder(
    val context: JavaOperationsContext,
    val fragment: IrFragmentDefinition,
) : JavaClassBuilder {
  private val packageName = context.layout.fragmentResponseFieldsPackageName(fragment.filePath)
  private val simpleName = context.layout.fragmentName(fragment.name).selections()

  override fun prepare() {
    context.resolver.registerFragmentSelections(
        fragment.name,
        ClassName.get(packageName, simpleName)
    )
  }

  override fun build(): CodegenJavaFile {
    return CodegenJavaFile(
        packageName = packageName,
        typeSpec = CompiledSelectionsBuilder(
            context = context,
        ).build(fragment.selectionSets, simpleName)
    )
  }
}
