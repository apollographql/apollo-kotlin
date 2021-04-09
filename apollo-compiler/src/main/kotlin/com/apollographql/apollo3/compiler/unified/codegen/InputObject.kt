package com.apollographql.apollo3.compiler.unified.codegen

import com.apollographql.apollo3.api.InputObject
import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.backend.codegen.adapterPackageName
import com.apollographql.apollo3.compiler.backend.codegen.kotlinNameForInputObjectAdapter
import com.apollographql.apollo3.compiler.backend.codegen.kotlinNameForInputObject
import com.apollographql.apollo3.compiler.backend.codegen.makeDataClass
import com.apollographql.apollo3.compiler.backend.codegen.suppressWarningsAnnotation
import com.apollographql.apollo3.compiler.unified.IrInputObject
import com.apollographql.apollo3.compiler.unified.codegen.adapter.inputAdapterTypeSpec
import com.apollographql.apollo3.compiler.unified.codegen.helpers.toNamedType
import com.apollographql.apollo3.compiler.unified.codegen.helpers.toParameterSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeSpec

internal fun IrInputObject.typeSpec() =
    TypeSpec
        .classBuilder(kotlinNameForInputObject(name))
        .applyIf(description?.isNotBlank()== true)  { addKdoc("%L\n", description!!) }
        .addAnnotation(suppressWarningsAnnotation)
        .makeDataClass(fields.map {
          it.toNamedType().toParameterSpec()
        })
        .addSuperinterface(InputObject::class)
        .build()

internal fun IrInputObject.typeName() =
    ClassName(
        packageName = packageName,
        kotlinNameForInputObject(name)
    )

internal fun IrInputObject.adapterTypeName() =
    ClassName(
        packageName = adapterPackageName(packageName),
        kotlinNameForInputObjectAdapter(name)
    )

internal fun IrInputObject.adapterTypeSpec(): TypeSpec {
  val adapterName = kotlinNameForInputObjectAdapter(name)

  return fields.map {
    it.toNamedType()
  }.inputAdapterTypeSpec(adapterName, typeName())
}

internal fun IrInputObject.qualifiedTypeSpecs(): List<ApolloFileSpec> {
  return listOf(
      ApolloFileSpec(packageName = packageName, typeSpec()),
      ApolloFileSpec(packageName = adapterPackageName(packageName), adapterTypeSpec())
  )
}
