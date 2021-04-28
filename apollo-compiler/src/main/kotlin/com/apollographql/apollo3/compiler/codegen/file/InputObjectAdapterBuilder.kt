package com.apollographql.apollo3.compiler.codegen.file

import com.apollographql.apollo3.compiler.codegen.CgContext
import com.apollographql.apollo3.compiler.codegen.CgFile
import com.apollographql.apollo3.compiler.codegen.CgFileBuilder
import com.apollographql.apollo3.compiler.codegen.adapter.inputAdapterTypeSpec
import com.apollographql.apollo3.compiler.codegen.helpers.toNamedType
import com.apollographql.apollo3.compiler.unified.ir.IrInputObject
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeSpec

class InputObjectAdapterBuilder(
    val context: CgContext,
    val inputObject: IrInputObject,
): CgFileBuilder {
  val packageName = context.layout.typeAdapterPackageName()
  val simpleName = context.layout.inputObjectAdapterName(inputObject.name)

  override fun prepare() {
    context.resolver.registerInputObjectAdapter(
        inputObject.name,
        ClassName(packageName, simpleName)
    )
  }

  override fun build(): CgFile {
    return CgFile(
        packageName = packageName,
        fileName = simpleName,
        typeSpecs = listOf(inputObject.adapterTypeSpec())
    )
  }

  private fun IrInputObject.adapterTypeSpec(): TypeSpec {
    return fields.map {
      it.toNamedType()
    }.inputAdapterTypeSpec(
        context,
        simpleName,
        context.resolver.resolveInputObject(inputObject.name)
    )
  }
}