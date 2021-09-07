package com.apollographql.apollo3.compiler.codegen.kotlin.file

import com.apollographql.apollo3.api.CompiledNamedType
import com.apollographql.apollo3.api.EnumType
import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.codegen.Identifier
import com.apollographql.apollo3.compiler.codegen.java.L
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinClassNames
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.deprecatedAnnotation
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.maybeAddDeprecation
import com.apollographql.apollo3.compiler.ir.IrEnum
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.maybeAddDescription
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.joinToCode

class EnumBuilder(
    private val context: KotlinContext,
    private val enum: IrEnum,
) : CgFileBuilder {
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
        typeSpecs = listOf(enum.toSealedClassTypeSpec())
    )
  }

  private fun IrEnum.toSealedClassTypeSpec(): TypeSpec {
    return TypeSpec
        .classBuilder(simpleName)
        .maybeAddDescription(description)
        // XXX: can an enum be made deprecated (and not only its values) ?
        .addModifiers(KModifier.SEALED)
        .primaryConstructor(primaryConstructorWithOverriddenParamSpec)
        .addProperty(rawValuePropertySpec)
        .addType(companionTypeSpec())
        .addTypes(values.map { value ->
          value.toObjectTypeSpec(ClassName("", layout.enumName(name)))
        })
        .addType(unknownValueTypeSpec())
        .build()
  }

  private fun IrEnum.companionTypeSpec(): TypeSpec {
    return TypeSpec.companionObjectBuilder()
        .addProperty(typePropertySpec())
        .addFunction(valueOfFunSpec())
        .build()
  }

  private fun IrEnum.Value.toObjectTypeSpec(superClass: TypeName): TypeSpec {
    return TypeSpec.objectBuilder(layout.enumValueName(name))
        .applyIf(description?.isNotBlank() == true) { addKdoc("%L\n", description!!) }
        .applyIf(deprecationReason != null) { addAnnotation(deprecatedAnnotation(deprecationReason!!)) }
        .superclass(superClass)
        .addSuperclassConstructorParameter("rawValue·=·%S", name)
        .build()
  }

  private fun IrEnum.unknownValueTypeSpec(): TypeSpec {
    return TypeSpec.classBuilder("UNKNOWN__")
        .addKdoc("%L", "An enum value that wasn't known at compile time.\n")
        .primaryConstructor(primaryConstructorSpec)
        .superclass(ClassName("", layout.enumName(name)))
        .addSuperclassConstructorParameter("rawValue·=·rawValue")
        .addFunction(
            FunSpec.builder("equals")
                .addModifiers(KModifier.OVERRIDE)
                .addParameter(ParameterSpec("other", KotlinClassNames.Any.copy(nullable = true)))
                .returns(KotlinClassNames.Boolean)
                .addCode("if·(other·!is·%T) return false\n", unknownValueClassName())
                .addCode("return·this.rawValue·==·other.rawValue")
                .build()
        )
        .addFunction(
            FunSpec.builder("hashCode")
                .addModifiers(KModifier.OVERRIDE)
                .returns(KotlinClassNames.Int)
                .addCode("return·this.rawValue.hashCode()")
                .build()
        )
        .addFunction(
            FunSpec.builder("toString")
                .addModifiers(KModifier.OVERRIDE)
                .returns(KotlinClassNames.String)
                .addCode("return·\"UNKNOWN__(${'$'}rawValue)\"")
                .build()
        )
        .build()
  }

  private fun IrEnum.valueOfFunSpec(): FunSpec {
    return FunSpec.builder("valueOf")
        .addKdoc("Returns [%T] matched with the specified [rawValue].\n", className())
        .addParameter("rawValue", String::class)
        .returns(className())
        .beginControlFlow("return·when(rawValue)")
        .addCode(
            values
                .map { CodeBlock.of("%S·->·%T", it.name, it.valueClassName()) }
                .joinToCode(separator = "\n", suffix = "\n")
        )
        .addCode("else -> %T(rawValue)\n", unknownValueClassName())
        .endControlFlow()
        .build()
  }

  private fun IrEnum.Value.valueClassName(): ClassName {
    return ClassName(packageName, simpleName, layout.enumValueName(name))
  }

  private fun unknownValueClassName(): ClassName {
    return ClassName(packageName, simpleName, "UNKNOWN__")
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
