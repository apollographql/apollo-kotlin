package com.apollographql.apollo3.compiler.codegen.java.file

import com.apollographql.apollo3.ast.GQLFragmentDefinition
import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.compiler.codegen.java.CodegenJavaFile
import com.apollographql.apollo3.compiler.codegen.java.JavaClassBuilder
import com.apollographql.apollo3.compiler.codegen.java.JavaContext
import com.apollographql.apollo3.compiler.codegen.java.selections.CompiledSelectionsBuilder
import com.apollographql.apollo3.compiler.ir.IrFragmentDefinition
import com.squareup.javapoet.ClassName

internal class FragmentSelectionsBuilder(
    val context: JavaContext,
    val fragment: IrFragmentDefinition,
) : JavaClassBuilder {
  private val packageName = context.layout.fragmentResponseFieldsPackageName(fragment.filePath)
  private val simpleName = context.layout.fragmentSelectionsName(fragment.name)

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
