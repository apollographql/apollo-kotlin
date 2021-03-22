package com.apollographql.apollo3.compiler.unified.codegen

import com.apollographql.apollo3.api.InputObject
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.backend.codegen.Identifier
import com.apollographql.apollo3.compiler.backend.codegen.kotlinNameForInputObjectAdapter
import com.apollographql.apollo3.compiler.backend.codegen.kotlinNameForInputObjectType
import com.apollographql.apollo3.compiler.backend.codegen.kotlinNameForVariablesAdapter
import com.apollographql.apollo3.compiler.backend.codegen.makeDataClass
import com.apollographql.apollo3.compiler.backend.codegen.obj
import com.apollographql.apollo3.compiler.backend.codegen.suppressWarningsAnnotation
import com.apollographql.apollo3.compiler.unified.IrInputField
import com.apollographql.apollo3.compiler.unified.IrInputObject
import com.apollographql.apollo3.compiler.unified.IrInputObjectType
import com.apollographql.apollo3.compiler.unified.codegen.helpers.NamedType
import com.apollographql.apollo3.compiler.unified.codegen.helpers.toParameterSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName

internal fun IrInputObject.typeSpec() =
    TypeSpec
        .classBuilder(kotlinNameForInputObjectType(name))
        .applyIf(description?.isNotBlank()== true)  { addKdoc("%L\n", description!!) }
        .addAnnotation(suppressWarningsAnnotation)
        .makeDataClass(fields.map {
          it.toNamedType().toParameterSpec()
        })
        .addSuperinterface(InputObject::class)
        .build()

internal fun IrInputObject.adapterTypeSpec(): TypeSpec {
  val adapterName = kotlinNameForInputObjectAdapter(name)
  val adaptedTypeName = IrInputObjectType(name).typeName()

  return fields.map {
    it.toNamedType()
  }.adapterTypeSpec(adapterName, adaptedTypeName)
}

fun IrInputField.toNamedType() = NamedType(
    graphQlName = name,
    type = type,
    optional = optional,
    description = description,
    deprecationReason = deprecationReason,
)

fun serializeVariablesFunSpec(
    funName: String,
    packageName: String,
    name: String,
): FunSpec {
  val serializerClassName = ClassName("$packageName.adapter", kotlinNameForVariablesAdapter(name))

  return FunSpec.builder(funName)
      .addModifiers(KModifier.OVERRIDE)
      .addParameter("writer", JsonWriter::class)
      .addParameter(Identifier.responseAdapterCache, ResponseAdapterCache::class.asTypeName())
      .addCode(
          "%L.toResponse(writer, ${Identifier.responseAdapterCache}, this)",
          CodeBlock.of("%T", serializerClassName).obj(false)
      )
      .build()
}
