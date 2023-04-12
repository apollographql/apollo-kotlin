package com.apollographql.apollo3.compiler.codegen.kotlin.file

import com.apollographql.apollo3.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.codegen.kotlin.adapter.inputAdapterTypeSpec
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.toNamedType
import com.apollographql.apollo3.compiler.ir.IrInputObject
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeSpec

internal class InputObjectAdapterBuilder(
    val context: KotlinContext,
    val inputObject: IrInputObject,
) : CgFileBuilder {
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
        context.resolver.resolveSchemaType(inputObject.name),
    )
  }
}
