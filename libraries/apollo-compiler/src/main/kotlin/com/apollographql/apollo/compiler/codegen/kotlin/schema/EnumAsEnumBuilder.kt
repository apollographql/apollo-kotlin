package com.apollographql.apollo.compiler.codegen.kotlin.schema

import com.apollographql.apollo.compiler.TargetLanguage
import com.apollographql.apollo.compiler.codegen.Identifier
import com.apollographql.apollo.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinSchemaContext
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.addSuppressions
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.deprecatedAnnotation
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.maybeAddDeprecation
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.maybeAddDescription
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.maybeAddOptIn
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.maybeAddRequiresOptIn
import com.apollographql.apollo.compiler.codegen.kotlin.schema.util.typePropertySpec
import com.apollographql.apollo.compiler.codegen.typePackageName
import com.apollographql.apollo.compiler.internal.escapeKotlinReservedWordInEnum
import com.apollographql.apollo.compiler.ir.IrEnum
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.joinToCode

internal class EnumAsEnumBuilder(
    private val context: KotlinSchemaContext,
    private val enum: IrEnum,
    private val withUnknown: Boolean
) : CgFileBuilder {
  private val layout = context.layout
  private val packageName = layout.typePackageName()
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
        typeSpecs = listOf(enum.toEnumClassTypeSpec())
    )
  }

  private fun IrEnum.toEnumClassTypeSpec(): TypeSpec {
    return TypeSpec.enumBuilder(simpleName)
        .maybeAddDescription(description)
        .primaryConstructor(primaryConstructorSpec)
        .addProperty(rawValuePropertySpec)
        .addType(companionTypeSpec())
        .apply {
          values.forEach { value ->
            addEnumConstant(value.targetName.escapeKotlinReservedWordInEnum(), value.enumConstTypeSpec())
          }
          if (withUnknown) {
            addEnumConstant("UNKNOWN__", unknownValueTypeSpec())
          }
        }
        .build()
  }

  private fun IrEnum.companionTypeSpec(): TypeSpec {
    return TypeSpec.companionObjectBuilder()
        .addProperty(typePropertySpec())
        .addProperty(knownEntriesPropertySpec())
        .apply {
          if (withUnknown) {
            // !withUnknown is a new thing, no need to add deprecated symbols there
            addFunction(knownValuesFunSpec())
          }
        }
        .addFunction(safeValueOfFunSpec())
        .build()
  }

  private fun IrEnum.knownValuesFunSpec(): FunSpec {
    return FunSpec.builder(Identifier.knownValues)
        .addKdoc("Returns all [%T] known at compile time", selfClassName)
        .addAnnotation(deprecatedAnnotation("Use knownEntries instead").toBuilder().addMember("replaceWith = ReplaceWith(%S)", "this.knownEntries").build())
        .returns(KotlinSymbols.Array.parameterizedBy(selfClassName))
        .addCode("return %N.toTypedArray()", Identifier.knownEntries)
        .build()
  }

  /**
   * `UNKNOWN__`, when present, is always added last, so the known entries are all the entries but the last one.
   *
   * Deriving this from `entries` instead of building a `listOf(...)` of every constant keeps the getter a
   * handful of bytes instead of growing linearly with the number of enum values, and returns a view instead
   * of allocating a new list on every call.
   */
  private fun IrEnum.knownEntriesPropertySpec(): PropertySpec {
    val code = if (withUnknown) {
      if (context.isTargetLanguageVersionAtLeast(TargetLanguage.KOTLIN_1_9)) {
        CodeBlock.of("return entries.subList(0, entries.size - 1)\n")
      } else {
        CodeBlock.of("return values().dropLast(1)\n")
      }
    } else {
      if (context.isTargetLanguageVersionAtLeast(TargetLanguage.KOTLIN_1_9)) {
        CodeBlock.of("return entries\n")
      } else {
        CodeBlock.of("return values().asList()\n")
      }
    }

    return PropertySpec.builder(Identifier.knownEntries, KotlinSymbols.List.parameterizedBy(selfClassName))
        .addKdoc("All [%T] known at compile time", selfClassName)
        .getter(
            FunSpec.getterBuilder()
                .addCode(code)
                .build()
        )
        .build()
  }

  /**
   * A `when` over the raw values instead of a linear `entries.find { }` scan: on the JVM this compiles to a
   * `hashCode` switch plus a single `equals`, so the lookup no longer walks the entries and no longer
   * allocates an iterator on every call. This matches what [EnumAsSealedInterfaceBuilder] already generates.
   */
  private fun IrEnum.safeValueOfFunSpec(): FunSpec {
    return FunSpec.builder("safeValueOf")
        .addKdoc(
            "Returns the [%T] that represents the specified [rawValue].\n" +
                "Note: unknown values of [rawValue] will return [UNKNOWN__]. You may want to update your schema instead of calling this function directly.\n",
            selfClassName
        )
        .addSuppressions(enum.values.any { it.deprecationReason != null })
        .maybeAddOptIn(context.resolver, enum.values)
        .addParameter("rawValue", KotlinSymbols.String)
        .returns(selfClassName)
        .beginControlFlow("return when(rawValue)")
        .addCode(
            values.map {
              CodeBlock.of("%S -> %N", it.name, it.targetName.escapeKotlinReservedWordInEnum())
            }.joinToCode(separator = "\n", suffix = "\n")
        )
        .apply {
          if (withUnknown) {
            addCode("else -> %N\n", Identifier.UNKNOWN__)
          } else {
            addCode("else -> error(\"No enum value found '${'$'}rawValue'\")\n")
          }
        }
        .endControlFlow()
        .build()
  }

  private fun IrEnum.Value.enumConstTypeSpec(): TypeSpec {
    return TypeSpec.anonymousClassBuilder()
        .maybeAddDeprecation(deprecationReason)
        .maybeAddRequiresOptIn(context.resolver, optInFeature)
        .maybeAddDescription(description)
        .addSuperclassConstructorParameter("%S", name)
        .build()
  }

  private fun unknownValueTypeSpec(): TypeSpec {
    return TypeSpec.anonymousClassBuilder()
        .addKdoc("%L", "Auto generated constant for unknown enum values\n")
        .addSuperclassConstructorParameter("%S", Identifier.UNKNOWN__)
        .build()
  }

  private val primaryConstructorSpec =
      FunSpec.constructorBuilder()
          .addParameter("rawValue", KotlinSymbols.String)
          .build()

  private val rawValuePropertySpec =
      PropertySpec.builder("rawValue", KotlinSymbols.String)
          .initializer("rawValue")
          .build()

}
