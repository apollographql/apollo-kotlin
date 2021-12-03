package com.apollographql.apollo3.compiler.codegen.kotlin.file

import com.apollographql.apollo3.compiler.codegen.Identifier
import com.apollographql.apollo3.compiler.codegen.Identifier.customScalarAdapters
import com.apollographql.apollo3.compiler.codegen.Identifier.root
import com.apollographql.apollo3.compiler.codegen.Identifier.rootField
import com.apollographql.apollo3.compiler.codegen.Identifier.selections
import com.apollographql.apollo3.compiler.codegen.Identifier.serializeVariables
import com.apollographql.apollo3.compiler.codegen.Identifier.toJson
import com.apollographql.apollo3.compiler.codegen.Identifier.writer
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinSymbols
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

fun serializeVariablesFunSpec(
    adapterClassName: TypeName?,
    emptyMessage: String,
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
      .addParameter(writer, KotlinSymbols.JsonWriter)
      .addParameter(customScalarAdapters, KotlinSymbols.CustomScalarAdapters)
      .addCode(body)
      .build()
}

fun adapterFunSpec(
    adapterTypeName: TypeName,
    adaptedTypeName: TypeName,
): FunSpec {
  return FunSpec.builder("adapter")
      .addModifiers(KModifier.OVERRIDE)
      .returns(KotlinSymbols.Adapter.parameterizedBy(adaptedTypeName))
      .addCode(CodeBlock.of("return·%T", adapterTypeName).obj(false))
      .build()
}

fun rootFieldFunSpec(context: KotlinContext, typeInScope: String, selectionsClassName: ClassName): FunSpec {
  return FunSpec.builder(rootField)
      .addModifiers(KModifier.OVERRIDE)
      .returns(KotlinSymbols.CompiledField)
      .addCode(
          CodeBlock.builder()
              .add("return %T(\n", KotlinSymbols.CompiledFieldBuilder)
              .indent()
              .add("name·=·%S,\n", Identifier.data)
              .add("type·=·%L\n", context.resolver.resolveCompiledType(typeInScope))
              .unindent()
              .add(")\n")
              .add(".$selections(selections·=·%T.$root)\n", selectionsClassName)
              .add(".build()\n")
              .build()
      )
      .build()
}

fun TypeSpec.maybeAddFilterNotNull(generateFilterNotNull: Boolean): TypeSpec {
  if (!generateFilterNotNull) {
    return this
  }
  return patchKotlinNativeOptionalArrayProperties()
}