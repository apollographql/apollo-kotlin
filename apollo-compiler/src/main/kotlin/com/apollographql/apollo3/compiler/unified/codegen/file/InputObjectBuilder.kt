package com.apollographql.apollo3.compiler.unified.codegen.file

import com.apollographql.apollo3.api.InputObject
import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.backend.codegen.makeDataClass
import com.apollographql.apollo3.compiler.backend.codegen.suppressWarningsAnnotation
import com.apollographql.apollo3.compiler.unified.codegen.CgContext
import com.apollographql.apollo3.compiler.unified.codegen.CgFile
import com.apollographql.apollo3.compiler.unified.codegen.CgFileBuilder
import com.apollographql.apollo3.compiler.unified.codegen.helpers.toNamedType
import com.apollographql.apollo3.compiler.unified.codegen.helpers.toParameterSpec
import com.apollographql.apollo3.compiler.unified.ir.IrInputObject
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeSpec

class InputObjectBuilder(
    val context: CgContext,
    val inputObject: IrInputObject
): CgFileBuilder {
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
    context.resolver.registerInputObject(
        inputObject.name,
        ClassName(packageName, simpleName)
    )
  }
  private fun IrInputObject.typeSpec() =
      TypeSpec
          .classBuilder(simpleName)
          .applyIf(description?.isNotBlank()== true)  { addKdoc("%L\n", description!!) }
          .addAnnotation(suppressWarningsAnnotation)
          .makeDataClass(fields.map {
            it.toNamedType().toParameterSpec(context)
          })
          .addSuperinterface(InputObject::class)
          .build()
}
