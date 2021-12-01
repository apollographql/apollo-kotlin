package com.apollographql.apollo3.compiler.codegen.kotlin.file

import com.apollographql.apollo3.compiler.codegen.Identifier.UNKNOWN__
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo3.compiler.codegen.kotlin.CgOutputFileBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.maybeAddDeprecation
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.maybeAddDescription
import com.apollographql.apollo3.compiler.ir.IrEnum
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec

class EnumAsEnumBuilder(
    private val context: KotlinContext,
    private val enum: IrEnum,
) : CgOutputFileBuilder {
  private val layout = context.layout
  private val packageName = layout.typePackageName()
  private val simpleName = layout.enumName(name = enum.name)

  private val selfClassName = ClassName(
      packageName,
      simpleName
  )

  override fun prepare() {
    context.resolver.registerSchemaType(
        enum.name,
        selfClassName
    )
  }

  override fun build(): CgFile {
    return CgFile(
        packageName = packageName,
        fileName = simpleName,
        typeSpecs = listOf(enum.toEnumClassTypeSpec())
    )
  }

  private fun IrEnum.toEnumClassTypeSpec(): TypeSpec {
    return TypeSpec
        .enumBuilder(simpleName)
        .maybeAddDescription(description)
        .primaryConstructor(primaryConstructorSpec)
        .addProperty(rawValuePropertySpec)
        .addType(companionTypeSpec())
        .apply {
          values.forEach { value ->
            addEnumConstant(layout.enumAsEnumValueName(value.name), value.enumConstTypeSpec())
          }
          addEnumConstant("UNKNOWN__", unknownValueTypeSpec())
        }
        .build()
  }

  private fun IrEnum.companionTypeSpec(): TypeSpec {
    return TypeSpec.companionObjectBuilder()
        .addProperty(typePropertySpec())
        .addFunction(safeValueOfFunSpec())
        .build()
  }

  private fun IrEnum.safeValueOfFunSpec(): FunSpec {
    return FunSpec
        .builder("safeValueOf")
        .addParameter("rawValue", String::
        class)
        .returns(ClassName("", name))
        .addStatement("return values().find·{·it.rawValue·==·rawValue·} ?: $UNKNOWN__")
        .build()
  }

  private fun IrEnum.Value.enumConstTypeSpec(): TypeSpec {
    return TypeSpec.anonymousClassBuilder()
        .maybeAddDeprecation(deprecationReason)
        .maybeAddDescription(description)
        .addSuperclassConstructorParameter("%S", name)
        .build()
  }

  private fun unknownValueTypeSpec(): TypeSpec {
    return TypeSpec
        .anonymousClassBuilder()
        .addKdoc("%L", "Auto generated constant for unknown enum values\n")
        .addSuperclassConstructorParameter("%S", UNKNOWN__)
        .build()
  }

  fun className(): TypeName {
    return ClassName(
        packageName,
        simpleName
    )
  }

  private val primaryConstructorSpec =
      FunSpec
          .constructorBuilder()
          .addParameter("rawValue", String::class)
          .build()

  private val rawValuePropertySpec =
      PropertySpec
          .builder("rawValue", String::class)
          .initializer("rawValue")
          .build()

}
