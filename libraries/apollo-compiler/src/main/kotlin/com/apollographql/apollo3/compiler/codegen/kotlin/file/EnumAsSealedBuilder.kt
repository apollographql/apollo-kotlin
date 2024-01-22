package com.apollographql.apollo3.compiler.codegen.kotlin.file

import com.apollographql.apollo3.compiler.codegen.Identifier.knownValues
import com.apollographql.apollo3.compiler.codegen.Identifier.safeValueOf
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.addSuppressions
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.maybeAddDeprecation
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.maybeAddDescription
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.maybeAddOptIn
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.maybeAddRequiresOptIn
import com.apollographql.apollo3.compiler.internal.escapeKotlinReservedWordInSealedClass
import com.apollographql.apollo3.compiler.ir.IrEnum
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.joinToCode

internal class EnumAsSealedBuilder(
    private val context: KotlinContext,
    private val enum: IrEnum,
) : CgFileBuilder {
  private val layout = context.layout
  private val packageName = "${layout.basePackageName()}.type"
  private val simpleName = layout.schemaTypeName(enum.name)

  private val selfClassName: ClassName
    get() = context.resolver.resolveSchemaType(enum.name)

  override fun prepare() {
    context.resolver.registerSchemaType(
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
        // XXX: can an enum be made deprecated (and not only its values) ?
        .addModifiers(KModifier.SEALED)
        .primaryConstructor(primaryConstructorWithOverriddenParamSpec)
        .addProperty(rawValuePropertySpec)
        .addType(companionTypeSpec())
        .addTypes(values.map { value ->
          value.toObjectTypeSpec(selfClassName)
        })
        .addType(unknownValueTypeSpec())
        .build()
  }

  private fun IrEnum.companionTypeSpec(): TypeSpec {
    return TypeSpec.companionObjectBuilder()
        .addProperty(typePropertySpec())
        .addFunction(safeValueOfFunSpec())
        .addFunction(knownValuesFunSpec())
        .build()
  }

  private fun IrEnum.Value.toObjectTypeSpec(superClass: TypeName): TypeSpec {
    return TypeSpec.objectBuilder(targetName.escapeKotlinReservedWordInSealedClass())
        .maybeAddDeprecation(deprecationReason)
        .maybeAddDescription(description)
        .maybeAddRequiresOptIn(context.resolver, optInFeature)
        .superclass(superClass)
        .addSuperclassConstructorParameter("rawValue·=·%S", name)
        .build()
  }

  private fun IrEnum.unknownValueTypeSpec(): TypeSpec {
    return TypeSpec.classBuilder("UNKNOWN__")
        .addKdoc("%L", "An enum value that wasn't known at compile time.\n")
        .primaryConstructor(primaryConstructorSpec)
        .superclass(selfClassName)
        .addSuperclassConstructorParameter("rawValue·=·rawValue")
        .addFunction(
            FunSpec.builder("equals")
                .addModifiers(KModifier.OVERRIDE)
                .addParameter(ParameterSpec("other", KotlinSymbols.Any.copy(nullable = true)))
                .returns(KotlinSymbols.Boolean)
                .addCode("if·(other·!is·%T) return false\n", unknownValueClassName())
                .addCode("return·this.rawValue·==·other.rawValue")
                .build()
        )
        .addFunction(
            FunSpec.builder("hashCode")
                .addModifiers(KModifier.OVERRIDE)
                .returns(KotlinSymbols.Int)
                .addCode("return·this.rawValue.hashCode()")
                .build()
        )
        .addFunction(
            FunSpec.builder("toString")
                .addModifiers(KModifier.OVERRIDE)
                .returns(KotlinSymbols.String)
                .addCode("return·\"UNKNOWN__(${'$'}rawValue)\"")
                .build()
        )
        .build()
  }

  private fun IrEnum.safeValueOfFunSpec(): FunSpec {
    return FunSpec.builder(safeValueOf)
        .addKdoc("Returns the [%T] that represents the specified [rawValue].\n", selfClassName)
        .addSuppressions(enum.values.any { it.deprecationReason != null })
        .maybeAddOptIn(context.resolver, enum.values)
        .addParameter("rawValue", KotlinSymbols.String)
        .returns(selfClassName)
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

  private fun IrEnum.knownValuesFunSpec(): FunSpec {
    return FunSpec.builder(knownValues)
        .addKdoc("Returns all [%T] known at compile time", selfClassName)
        .addSuppressions(enum.values.any { it.deprecationReason != null })
        .maybeAddOptIn(context.resolver, enum.values)
        .returns(KotlinSymbols.Array.parameterizedBy(selfClassName))
        .addCode(
            CodeBlock.builder()
                .add("return·arrayOf(\n")
                .indent()
                .add(
                    values.map {
                      CodeBlock.of("%T", it.valueClassName())
                    }.joinToCode(",\n")
                )
                .unindent()
                .add(")\n")
                .build()
        )
        .build()
  }

  private fun IrEnum.Value.valueClassName(): ClassName {
    return ClassName(selfClassName.packageName, selfClassName.simpleName, targetName.escapeKotlinReservedWordInSealedClass())
  }

  private fun unknownValueClassName(): ClassName {
    return ClassName(selfClassName.packageName, selfClassName.simpleName, "UNKNOWN__")
  }

  private val primaryConstructorSpec =
      FunSpec
          .constructorBuilder()
          .addParameter("rawValue", KotlinSymbols.String)
          .build()

  private val primaryConstructorWithOverriddenParamSpec =
      FunSpec
          .constructorBuilder()
          .addParameter("rawValue", KotlinSymbols.String)
          .build()

  private val rawValuePropertySpec =
      PropertySpec
          .builder("rawValue", KotlinSymbols.String)
          .initializer("rawValue")
          .build()

}
