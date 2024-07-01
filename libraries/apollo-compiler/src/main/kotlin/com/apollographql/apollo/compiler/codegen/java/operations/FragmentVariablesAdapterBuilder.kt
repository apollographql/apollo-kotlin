package com.apollographql.apollo.compiler.codegen.java.operations

import com.apollographql.apollo.compiler.codegen.fragmentAdapterPackageName
import com.apollographql.apollo.compiler.codegen.impl
import com.apollographql.apollo.compiler.codegen.java.CodegenJavaFile
import com.apollographql.apollo.compiler.codegen.java.JavaClassBuilder
import com.apollographql.apollo.compiler.codegen.java.JavaOperationsContext
import com.apollographql.apollo.compiler.codegen.java.operations.util.variableAdapterTypeSpec
import com.apollographql.apollo.compiler.codegen.variablesAdapter
import com.apollographql.apollo.compiler.ir.IrFragmentDefinition
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeSpec

internal class FragmentVariablesAdapterBuilder(
    val context: JavaOperationsContext,
    val fragment: IrFragmentDefinition,
) : JavaClassBuilder {
  private val packageName = context.layout.fragmentAdapterPackageName(fragment.filePath)
  private val simpleName = context.layout.fragmentName(fragment.name).impl().variablesAdapter()

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
    return fragment.variables
        .variableAdapterTypeSpec(
            context = context,
            adapterName = simpleName,
            adaptedTypeName = context.resolver.resolveFragment(fragment.name),
        )
  }
}
