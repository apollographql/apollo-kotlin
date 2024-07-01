package com.apollographql.apollo.compiler.codegen.kotlin.operations

import com.apollographql.apollo.compiler.codegen.fragmentAdapterPackageName
import com.apollographql.apollo.compiler.codegen.impl
import com.apollographql.apollo.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinOperationsContext
import com.apollographql.apollo.compiler.codegen.kotlin.operations.util.variablesAdapterTypeSpec
import com.apollographql.apollo.compiler.codegen.variablesAdapter
import com.apollographql.apollo.compiler.ir.IrFragmentDefinition
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeSpec

internal class FragmentVariablesAdapterBuilder(
    val context: KotlinOperationsContext,
    val fragment: IrFragmentDefinition,
) : CgFileBuilder {
  private val packageName = context.layout.fragmentAdapterPackageName(fragment.filePath)
  private val simpleName = context.layout.fragmentName(fragment.name).impl().variablesAdapter()

  override fun prepare() {
    context.resolver.registerFragmentVariablesAdapter(
        fragment.name,
        ClassName(packageName, simpleName)
    )
  }

  override fun build(): CgFile {
    return CgFile(
        packageName = packageName,
        fileName = simpleName,
        typeSpecs = listOf(typeSpec())
    )
  }

  private fun typeSpec(): TypeSpec {
    return fragment.variables
        .variablesAdapterTypeSpec(
            context = context,
            adapterName = simpleName,
            adaptedTypeName = context.resolver.resolveFragment(fragment.name),
        )
  }
}