/*
 * Generates ResponseAdapters for variables/input
 */
package com.apollographql.apollo3.compiler.codegen.kotlin.adapter

import com.apollographql.apollo3.compiler.codegen.Identifier
import com.apollographql.apollo3.compiler.codegen.Identifier.fromJson
import com.apollographql.apollo3.compiler.codegen.Identifier.scalarAdapters
import com.apollographql.apollo3.compiler.codegen.Identifier.toJson
import com.apollographql.apollo3.compiler.codegen.Identifier.value
import com.apollographql.apollo3.compiler.codegen.Identifier.writer
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.NamedType
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.requiresOptInAnnotation
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.suppressDeprecationAnnotationSpec
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.writeToResponseCodeBlock
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec

internal fun List<NamedType>.inputAdapterTypeSpec(
    context: KotlinContext,
    adapterName: String,
    adaptedTypeName: TypeName,
    withDefaultBooleanValues: Boolean,
): TypeSpec {
  return TypeSpec.objectBuilder(adapterName)
      .addSuperinterface(KotlinSymbols.ApolloAdapter.parameterizedBy(adaptedTypeName))
      .addFunction(notImplementedFromResponseFunSpec(adaptedTypeName))
      .addFunction(writeToResponseFunSpec(context, adaptedTypeName, withDefaultBooleanValues))
      .apply {
        if (this@inputAdapterTypeSpec.any { it.deprecationReason != null }) {
          addAnnotation(suppressDeprecationAnnotationSpec)
        }
        if (any { it.optInFeature != null }) {
          val requiresOptInAnnotation = context.resolver.resolveRequiresOptInAnnotation()
          if (requiresOptInAnnotation != null) {
            addAnnotation(requiresOptInAnnotation(requiresOptInAnnotation))
          }
        }
      }
      .build()
}

private fun notImplementedFromResponseFunSpec(adaptedTypeName: TypeName) = FunSpec.builder(fromJson)
    .addModifiers(KModifier.OVERRIDE)
    .addParameter(Identifier.reader, KotlinSymbols.JsonReader)
    .addParameter(scalarAdapters, KotlinSymbols.ScalarAdapters)
    .returns(adaptedTypeName)
    .addCode("throw %T(%S)", ClassName("kotlin", "IllegalStateException"), "Input type used in output position")
    .build()


private fun List<NamedType>.writeToResponseFunSpec(
    context: KotlinContext,
    adaptedTypeName: TypeName,
    withDefaultBooleanValues: Boolean,
): FunSpec {
  return FunSpec.builder(toJson)
      .addModifiers(KModifier.OVERRIDE)
      .addParameter(writer, KotlinSymbols.JsonWriter)
      .addParameter(scalarAdapters, KotlinSymbols.ScalarAdapters)
      .addParameter(value, adaptedTypeName)
      .addCode(writeToResponseCodeBlock(context, withDefaultBooleanValues))
      .build()
}


