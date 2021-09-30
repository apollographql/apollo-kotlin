package com.apollographql.apollo3.compiler.codegen.java.file

import com.apollographql.apollo3.compiler.codegen.java.CodegenJavaFile
import com.apollographql.apollo3.compiler.codegen.java.JavaClassBuilder
import com.apollographql.apollo3.compiler.codegen.java.JavaContext
import com.apollographql.apollo3.compiler.codegen.java.adapter.inputAdapterTypeSpec
import com.apollographql.apollo3.compiler.codegen.java.helpers.toNamedType
import com.apollographql.apollo3.compiler.ir.IrNamedFragment
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeSpec

class FragmentVariablesAdapterBuilder(
    val context: JavaContext,
    val fragment: IrNamedFragment
): JavaClassBuilder {
  private val packageName = context.layout.fragmentAdapterPackageName(fragment.filePath)
  private val simpleName = context.layout.fragmentVariablesAdapterName(fragment.name)

  override fun prepare() {
    context.resolver.registerFragmentVariablesAdapter(
        fragment.name,
        ClassName.get(packageName, simpleName)
    )
  }

  override fun build(): CodegenJavaFile {
    return CodegenJavaFile(
        packageName = packageName,
        typeSpec = typeSpec()
    )
  }

  private fun typeSpec(): TypeSpec {
    return fragment.variables.map { it.toNamedType() }
        .inputAdapterTypeSpec(
            context = context,
            adapterName = simpleName,
            adaptedTypeName = context.resolver.resolveFragment(fragment.name)
        )
  }
}