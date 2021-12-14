package com.apollographql.apollo3.compiler.codegen.java.file

import com.apollographql.apollo3.compiler.codegen.java.CodegenJavaFile
import com.apollographql.apollo3.compiler.codegen.java.JavaClassBuilder
import com.apollographql.apollo3.compiler.codegen.java.JavaContext
import com.apollographql.apollo3.compiler.codegen.java.adapter.ResponseAdapterBuilder
import com.apollographql.apollo3.compiler.codegen.maybeFlatten
import com.apollographql.apollo3.compiler.ir.IrNamedFragment
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

class FragmentDataAdapterBuilder(
    val context: JavaContext,
    val fragment: IrNamedFragment,
    val flatten: Boolean,
) : JavaClassBuilder {
  private val packageName = context.layout.fragmentPackageName(fragment.filePath)
  private val simpleName = context.layout.fragmentResponseAdapterWrapperName(fragment.name)

  private val responseAdapterBuilders = fragment.dataModelGroup.maybeFlatten(flatten).map {
    ResponseAdapterBuilder.create(
        context = context,
        modelGroup = it,
        path = listOf(packageName, simpleName),
        true
    )
  }

  override fun prepare() {
    responseAdapterBuilders.forEach { it.prepare() }
  }

  override fun build(): CodegenJavaFile {
    return CodegenJavaFile(
        packageName = packageName,
        typeSpec = fragment.responseAdapterTypeSpec()
    )
  }

  private fun IrNamedFragment.responseAdapterTypeSpec(): TypeSpec {
    return TypeSpec.classBuilder(simpleName)
        .addModifiers(Modifier.PUBLIC)
        .addTypes(
            responseAdapterBuilders.flatMap { it.build() }
        )
        .build()
  }
}