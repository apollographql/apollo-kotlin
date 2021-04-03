package com.apollographql.apollo3.compiler.unified.codegen.file

import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.backend.codegen.deprecatedAnnotation
import com.apollographql.apollo3.compiler.unified.codegen.CgContext
import com.apollographql.apollo3.compiler.unified.codegen.CgLayout
import com.apollographql.apollo3.compiler.unified.codegen.CgFile
import com.apollographql.apollo3.compiler.unified.codegen.CgFileBuilder
import com.apollographql.apollo3.compiler.unified.ir.IrEnum
import com.apollographql.apollo3.compiler.unified.codegen.helpers.maybeAddDescription
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec

class EnumBuilder(
    private val context: CgContext,
    private val enum: IrEnum
): CgFileBuilder {
  private val layout = context.layout
  private val packageName = layout.typePackageName()
  private val simpleName = layout.enumName(name = enum.name)

  override fun prepare() {
    context.resolver.registerEnum(
        enum.name,
        ClassName(
            packageName,
            simpleName
        )
    )
  }
  override fun build(): CgFile {
    return CgFile(
        packageName = packageName,
        fileName = simpleName,
        typeSpecs = listOf(enum.toSealedClassTypeSpec())
    )
  }

  private fun IrEnum.toSealedClassTypeSpec(): TypeSpec {
    return TypeSpec
        .classBuilder(simpleName)
        .maybeAddDescription(description)
        .addModifiers(KModifier.SEALED)
        .primaryConstructor(primaryConstructorWithOverriddenParamSpec)
        .addProperty(rawValuePropertySpec)
        .addTypes(values.map { value ->
          value.toObjectTypeSpec(ClassName("", layout.enumName(name)))
        })
        .addType(unknownValueTypeSpec())
        .build()
  }

  private fun IrEnum.Value.toObjectTypeSpec(superClass: TypeName): TypeSpec {
    return TypeSpec.objectBuilder(layout.enumValueName(name))
        .applyIf(description?.isNotBlank() == true) { addKdoc("%L\n", description!!) }
        .applyIf(deprecationReason != null) { addAnnotation(deprecatedAnnotation(deprecationReason!!)) }
        .superclass(superClass)
        .addSuperclassConstructorParameter("rawValue = %S", name)
        .build()
  }

  private fun IrEnum.unknownValueTypeSpec(): TypeSpec {
    return TypeSpec.classBuilder("UNKNOWN__")
        .addKdoc("%L", "Auto generated constant for unknown enum values\n")
        .primaryConstructor(primaryConstructorSpec)
        .superclass(ClassName("", layout.enumName(name)))
        .addSuperclassConstructorParameter("rawValue = rawValue")
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

  private val primaryConstructorWithOverriddenParamSpec =
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
