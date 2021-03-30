package com.apollographql.apollo3.compiler.unified.codegen

import com.apollographql.apollo3.api.InputObject
import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.backend.codegen.makeDataClass
import com.apollographql.apollo3.compiler.backend.codegen.suppressWarningsAnnotation
import com.apollographql.apollo3.compiler.unified.ClassLayout
import com.apollographql.apollo3.compiler.unified.IrInputObject
import com.apollographql.apollo3.compiler.unified.codegen.adapter.inputAdapterTypeSpec
import com.apollographql.apollo3.compiler.unified.codegen.helpers.toNamedType
import com.apollographql.apollo3.compiler.unified.codegen.helpers.toParameterSpec
import com.squareup.kotlinpoet.TypeSpec

internal fun IrInputObject.typeSpec(layout: ClassLayout) =
    TypeSpec
        .classBuilder(layout.inputObjectName(name))
        .applyIf(description?.isNotBlank()== true)  { addKdoc("%L\n", description!!) }
        .addAnnotation(suppressWarningsAnnotation)
        .makeDataClass(fields.map {
          it.toNamedType().toParameterSpec(layout)
        })
        .addSuperinterface(InputObject::class)
        .build()

internal fun IrInputObject.adapterTypeSpec(layout: ClassLayout): TypeSpec {
  val adapterName = layout.inputObjectAdapterName(name)

  return fields.map {
    it.toNamedType()
  }.inputAdapterTypeSpec(layout, adapterName, layout.inputObjectClassName(name))
}

internal fun IrInputObject.qualifiedTypeSpecs(layout: ClassLayout): List<ApolloFileSpec> {
  return listOf(
      ApolloFileSpec(packageName = layout.typePackageName(), typeSpec(layout)),
      ApolloFileSpec(packageName = layout.typeAdapterPackageName(), adapterTypeSpec(layout))
  )
}
