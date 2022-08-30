package com.apollographql.apollo3.compiler.codegen.kotlin.file

import com.apollographql.apollo3.compiler.codegen.Identifier
import com.apollographql.apollo3.compiler.codegen.Identifier.PossibleTypes
import com.apollographql.apollo3.compiler.codegen.Identifier.UNKNOWN__
import com.apollographql.apollo3.compiler.codegen.Identifier.safeValueOf
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinCodegenLayout
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo3.compiler.ir.IrCompositeType2
import com.apollographql.apollo3.compiler.ir.IrModel
import com.apollographql.apollo3.compiler.ir.IrNonNullType2
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

internal fun possibleTypesEnumTypeName(polymorphicTypeName: String): String {
  return "$polymorphicTypeName.$PossibleTypes"
}

internal fun possibleTypesEnumTypeSpec(
    layout: KotlinCodegenLayout,
    polymorphicTypeName: String,
    compiledName: String,
    possibleTypes: List<String>
): TypeSpec {
  return TypeSpec.enumBuilder(compiledName)
      .addKdoc("%L", "Auto generated enum class for enumerable possible type of $polymorphicTypeName\n")
      .primaryConstructor(primaryConstructorSpec)
      .addProperty(
          PropertySpec
              .builder("rawValue", KotlinSymbols.String)
              .initializer("rawValue")
              .build()
      )
      .apply {
        possibleTypes.forEach { typename ->
          addEnumConstant(layout.enumAsEnumValueName(typename), enumSpec(typename))
        }
        addEnumConstant(UNKNOWN__, unknownValueTypeSpec())
      }
      .addType(
          TypeSpec.companionObjectBuilder()
              .addFunction(possibleTypesEnumSafeValueOfFunSpec(compiledName))
              .build()
      )
      .build()
}

private val primaryConstructorSpec =
    FunSpec
        .constructorBuilder()
        .addParameter("rawValue", KotlinSymbols.String)
        .build()

private fun enumSpec(typename: String) = TypeSpec.anonymousClassBuilder()
    .addSuperclassConstructorParameter("%S", typename)
    .build()

private fun possibleTypesEnumSafeValueOfFunSpec(compiledName: String): FunSpec {
  return FunSpec.builder(safeValueOf)
      .addParameter("rawValue", String::class)
      .returns(ClassName("", compiledName))
      .addStatement("return·values().find·{·it.rawValue·==·rawValue·} ?: $UNKNOWN__")
      .build()
}

private fun unknownValueTypeSpec(): TypeSpec {
  return TypeSpec
      .anonymousClassBuilder()
      .addKdoc("%L", "Auto generated constant for unknown __typename values\n")
      .addSuperclassConstructorParameter("%S", UNKNOWN__)
      .build()
}

internal fun IrModel.shouldAddPossibleTypesEnumField(): Boolean {
  return polymorphicTypeName != null
      && properties.any { it.info.responseName == Identifier.__typename }
}

internal fun TypeSpec.Builder.addPossibleTypesEnumField(
    context: KotlinContext,
    model: IrModel
): TypeSpec.Builder {
  return (context.resolver.resolveIrType2(
          IrNonNullType2(IrCompositeType2(possibleTypesEnumTypeName(model.polymorphicTypeName!!))))
      as? ClassName)?.let { possibleEnumType ->
    addProperty(PropertySpec.builder("__type", possibleEnumType)
        .addKdoc("%L", "Synthetic field for enumerable possible type of ${model.polymorphicTypeName}\n")
        .getter(FunSpec.getterBuilder()
            .addCode("return ${possibleEnumType.simpleName}.safeValueOf(${Identifier.__typename})")
            .build()
        )
        .build()
    )
  } ?: this
}