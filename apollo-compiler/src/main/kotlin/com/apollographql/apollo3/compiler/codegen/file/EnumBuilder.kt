package com.apollographql.apollo3.compiler.codegen.file

import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.codegen.CgContext
import com.apollographql.apollo3.compiler.codegen.CgFile
import com.apollographql.apollo3.compiler.codegen.CgFileBuilder
import com.apollographql.apollo3.compiler.codegen.helpers.deprecatedAnnotation
import com.apollographql.apollo3.compiler.ir.IrEnum
import com.apollographql.apollo3.compiler.codegen.helpers.maybeAddDescription
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.joinToCode

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
        .addType(companionObjectSpec())
        .build()
  }

  private fun IrEnum.Value.toObjectTypeSpec(superClass: TypeName): TypeSpec {
    return TypeSpec.objectBuilder(valueClassName())
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

  private fun IrEnum.companionObjectSpec(): TypeSpec {
    return TypeSpec.companionObjectBuilder()
        .addFunction(valueOfFunSpec())
        .build()
  }

  private fun IrEnum.valueOfFunSpec(): FunSpec {
    return FunSpec.builder("valueOf")
        .addKdoc("Returns [%T] matched with the specified [rawValue].\n", className())
        .addParameter("rawValue", String::class)
        .returns(className())
        .beginControlFlow("return when(rawValue)")
        .addCode(
            values
                .map { CodeBlock.of("%S -> %T", it.name, it.valueClassName()) }
                .joinToCode(separator = "\n", suffix = "\n")
        )
        .addCode("else -> UNKNOWN__(rawValue)\n")
        .endControlFlow()
        .build()
  }

  private fun IrEnum.Value.valueClassName(): ClassName {
    return ClassName(packageName, simpleName, layout.enumValueName(name))
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
