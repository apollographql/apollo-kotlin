package com.apollographql.apollo3.compiler.codegen.file

import com.apollographql.apollo3.compiler.codegen.CgContext
import com.apollographql.apollo3.compiler.codegen.CgFile
import com.apollographql.apollo3.compiler.codegen.CgFileBuilder
import com.apollographql.apollo3.compiler.ir.IrNamedFragment
import com.apollographql.apollo3.compiler.codegen.adapter.inputAdapterTypeSpec
import com.apollographql.apollo3.compiler.codegen.helpers.toNamedType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeSpec

class FragmentVariablesAdapterBuilder(
    val context: CgContext,
    val fragment: IrNamedFragment
): CgFileBuilder {
  private val packageName = context.layout.fragmentAdapterPackageName(fragment.filePath)
  private val simpleName = context.layout.fragmentVariablesAdapterName(fragment.name)

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
    return fragment.variables.map { it.toNamedType() }
        .inputAdapterTypeSpec(
            context = context,
            adapterName = simpleName,
            adaptedTypeName = context.resolver.resolveFragment(fragment.name)
        )
  }
}