package com.apollographql.apollo.compiler.codegen.kotlin.schema

import com.apollographql.apollo.compiler.codegen.inputAdapter
import com.apollographql.apollo.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinSchemaContext
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.inputAdapterTypeSpec
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.toNamedType
import com.apollographql.apollo.compiler.codegen.typeAdapterPackageName
import com.apollographql.apollo.compiler.ir.IrInputObject
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeSpec

internal class InputObjectAdapterBuilder(
    val context: KotlinSchemaContext,
    val inputObject: IrInputObject,
) : CgFileBuilder {
  val packageName = context.layout.typeAdapterPackageName()
  val simpleName = context.layout.schemaTypeName(inputObject.name).inputAdapter()

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
