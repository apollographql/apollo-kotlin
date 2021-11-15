package com.apollographql.apollo3.compiler.codegen.kotlin.file

import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.CompiledSelection
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.compiler.codegen.Identifier.customScalarAdapters
import com.apollographql.apollo3.compiler.codegen.Identifier.selections
import com.apollographql.apollo3.compiler.codegen.Identifier.serializeVariables
import com.apollographql.apollo3.compiler.codegen.Identifier.toJson
import com.apollographql.apollo3.compiler.codegen.Identifier.writer
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinClassNames
import com.apollographql.apollo3.compiler.codegen.kotlin.adapter.obj
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.patchKotlinNativeOptionalArrayProperties
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName

fun serializeVariablesFunSpec(
    adapterClassName: TypeName?,
    emptyMessage: String
): FunSpec {

  val body = if (adapterClassName == null) {
    CodeBlock.of("""
      // $emptyMessage
    """.trimIndent())
  } else {
    CodeBlock.of(
        "%L.$toJson($writer, $customScalarAdapters, this)",
            CodeBlock.of("%T", adapterClassName)
    )
  }
  return FunSpec.builder(serializeVariables)
      .addModifiers(KModifier.OVERRIDE)
      .addParameter(writer, KotlinClassNames.JsonWriter)
      .addParameter(customScalarAdapters, KotlinClassNames.CustomScalarAdapters)
      .addCode(body)
      .build()
}

fun adapterFunSpec(
    adapterTypeName: TypeName,
    adaptedTypeName: TypeName
): FunSpec {
  return FunSpec.builder("adapter")
      .addModifiers(KModifier.OVERRIDE)
      .returns(KotlinClassNames.Adapter.parameterizedBy(adaptedTypeName))
      .addCode(CodeBlock.of("return·%T", adapterTypeName).obj(false))
      .build()
}

fun selectionsFunSpec(context: KotlinContext, className: ClassName): FunSpec {
  return FunSpec.builder(selections)
      .addModifiers(KModifier.OVERRIDE)
      .returns(KotlinClassNames.List.parameterizedBy(KotlinClassNames.CompiledSelection))
      .addCode("return %T.%L\n", className, context.layout.rootSelectionsPropertyName())
      .build()
}

fun TypeSpec.maybeAddFilterNotNull(generateFilterNotNull: Boolean): TypeSpec {
  if (!generateFilterNotNull) {
    return this
  }
  return patchKotlinNativeOptionalArrayProperties()
}