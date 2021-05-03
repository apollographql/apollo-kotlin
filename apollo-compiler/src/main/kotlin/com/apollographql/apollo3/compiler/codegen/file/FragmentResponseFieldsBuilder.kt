package com.apollographql.apollo3.compiler.codegen.file

import com.apollographql.apollo3.compiler.codegen.CgContext
import com.apollographql.apollo3.compiler.codegen.CgFile
import com.apollographql.apollo3.compiler.codegen.CgFileBuilder
import com.apollographql.apollo3.compiler.unified.ir.IrNamedFragment
import com.apollographql.apollo3.compiler.codegen.responsefields.ResponseFieldsBuilder
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeSpec

class FragmentResponseFieldsBuilder(
    val context: CgContext,
    val fragment: IrNamedFragment,
) : CgFileBuilder {
  private val packageName = context.layout.fragmentResponseFieldsPackageName(fragment.filePath)
  private val simpleName = context.layout.fragmentResponseFieldsName(fragment.name)

  override fun prepare() {
    context.resolver.registerFragmentMergedFields(
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
    return ResponseFieldsBuilder(
        rootField = fragment.field,
        rootName = simpleName
    ).build()
  }
}