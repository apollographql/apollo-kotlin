package com.apollographql.apollo3.compiler.codegen.kotlin.file

import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.CgOutputFileBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.makeDataClass
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.toNamedType
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.toParameterSpec
import com.apollographql.apollo3.compiler.ir.IrInputObject
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeSpec

class InputObjectBuilder(
    val context: KotlinContext,
    val inputObject: IrInputObject
): CgOutputFileBuilder {
  private val packageName = context.layout.typePackageName()
  private val simpleName = context.layout.inputObjectName(inputObject.name)

  override fun build(): CgFile {
    return CgFile(
        packageName = packageName,
        fileName = simpleName,
        typeSpecs = listOf(inputObject.typeSpec())
    )
  }

  override fun prepare() {
    context.resolver.registerSchemaType(
        inputObject.name,
        ClassName(packageName, simpleName)
    )
  }
  private fun IrInputObject.typeSpec() =
      TypeSpec
          .classBuilder(simpleName)
          .applyIf(description?.isNotBlank()== true)  { addKdoc("%L\n", description!!) }
          .makeDataClass(fields.map {
            it.toNamedType().toParameterSpec(context)
          })
          .build()
}
