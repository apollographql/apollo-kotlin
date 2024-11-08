package com.apollographql.apollo.compiler.codegen.kotlin.schema

import com.apollographql.apollo.compiler.codegen.Identifier
import com.apollographql.apollo.compiler.codegen.Identifier.KNOWN__
import com.apollographql.apollo.compiler.codegen.Identifier.UNKNOWN__
import com.apollographql.apollo.compiler.codegen.Identifier.rawValue
import com.apollographql.apollo.compiler.codegen.Identifier.safeValueOf
import com.apollographql.apollo.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinSchemaContext
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.addSuppressions
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.maybeAddDeprecation
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.maybeAddDescription
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.maybeAddOptIn
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.maybeAddRequiresOptIn
import com.apollographql.apollo.compiler.codegen.kotlin.schema.util.typePropertySpec
import com.apollographql.apollo.compiler.codegen.typePackageName
import com.apollographql.apollo.compiler.internal.escapeKotlinReservedWordInSealedClass
import com.apollographql.apollo.compiler.ir.IrEnum
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.buildCodeBlock
import com.squareup.kotlinpoet.joinToCode
import com.squareup.kotlinpoet.withIndent

internal class EnumAsSealedInterfaceBuilder(
    private val context: KotlinSchemaContext,
    private val enum: IrEnum,
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
        typeSpecs = listOf(enum.toSealedInterfaceTypeSpec())
    )
  }

  private fun IrEnum.toSealedInterfaceTypeSpec(): TypeSpec {
    return TypeSpec.interfaceBuilder(simpleName)
        .maybeAddDescription(description)
        .addModifiers(KModifier.SEALED)
        .addProperty(
            PropertySpec.builder(rawValue, KotlinSymbols.String)
            .build()
        )
        .addType(companionTypeSpec())
        .addTypes(values.map { value ->
          value.toObjectTypeSpec()
        })
        .addType(knownValueTypeSpec())
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

  private fun IrEnum.Value.toObjectTypeSpec(): TypeSpec {
    return TypeSpec.objectBuilder(targetName.escapeKotlinReservedWordInSealedClass())
        .maybeAddDeprecation(deprecationReason)
        .maybeAddDescription(description)
        .maybeAddRequiresOptIn(context.resolver, optInFeature)
        .addSuperinterface(selfClassName.nestedClass(KNOWN__))
        .addProperty(
            PropertySpec.builder("rawValue", KotlinSymbols.String)
                .addModifiers(KModifier.OVERRIDE)
                .initializer("%S", name)
                .build()
        )
        .build()
  }

  private fun IrEnum.knownValueTypeSpec(): TypeSpec {
    return TypeSpec.interfaceBuilder(KNOWN__)
        .addKdoc("An enum value that is known at build time.")
        .addSuperinterface(selfClassName)
        .addProperty(
            PropertySpec.builder(rawValue, KotlinSymbols.String)
                .addModifiers(KModifier.OVERRIDE)
                .build()
        )
        .addModifiers(KModifier.SEALED)
        .addAnnotation(AnnotationSpec.builder(KotlinSymbols.Suppress).addMember("%S", "ClassName").build())
        .build()
  }

  private fun IrEnum.unknownValueTypeSpec(): TypeSpec {
    return TypeSpec.classBuilder(UNKNOWN__)
        .addKdoc("An enum value that isn't known at build time.")
        .addSuperinterface(selfClassName)
        .primaryConstructor(
            FunSpec.constructorBuilder()
                .addAnnotation(AnnotationSpec.builder(KotlinSymbols.ApolloPrivateEnumConstructor).build())
                .addParameter(rawValue, KotlinSymbols.String)
                .build()
        )
        .addProperty(
            PropertySpec.builder(rawValue, KotlinSymbols.String)
                .addModifiers(KModifier.OVERRIDE)
                .initializer(rawValue)
                .build()
        )
        .addAnnotation(AnnotationSpec.builder(KotlinSymbols.Suppress).addMember("%S", "ClassName").build())
        .addFunction(
            FunSpec.builder("equals")
                .addModifiers(KModifier.OVERRIDE)
                .addParameter(ParameterSpec("other", KotlinSymbols.Any.copy(nullable = true)))
                .returns(KotlinSymbols.Boolean)
                .addCode("if (other !is $UNKNOWN__) return false\n",)
                .addCode("return this.$rawValue == other.rawValue")
                .build()
        )
        .addFunction(
            FunSpec.builder("hashCode")
                .addModifiers(KModifier.OVERRIDE)
                .returns(KotlinSymbols.Int)
                .addCode("return this.$rawValue.hashCode()")
                .build()
        )
        .addFunction(
            FunSpec.builder("toString")
                .addModifiers(KModifier.OVERRIDE)
                .returns(KotlinSymbols.String)
                .addCode("return \"$UNKNOWN__(${'$'}$rawValue)\"")
                .build()
        )
        .build()
  }

  private fun IrEnum.safeValueOfFunSpec(): FunSpec {
    return FunSpec.builder(safeValueOf)
        .addKdoc(
            """
            Returns an instance of [%T] representing [$rawValue].
            
            The returned value may be an instance of [$UNKNOWN__] if the enum value is not known at build time. 
            You may want to update your schema instead of calling this function directly.
            """.trimIndent(),
            selfClassName
        )
        .addSuppressions(enum.values.any { it.deprecationReason != null })
        .maybeAddOptIn(context.resolver, enum.values)
        .addParameter(rawValue, KotlinSymbols.String)
        .returns(selfClassName)
        .beginControlFlow("return when($rawValue)")
        .addCode(
            values
                .map { CodeBlock.of("%S -> %T", it.name, it.valueClassName()) }
                .joinToCode(separator = "\n", suffix = "\n")
        )
        .addCode(buildCodeBlock {
          add("else -> {\n")
          withIndent {
            add("@%T(%T::class)\n", KotlinSymbols.OptIn, KotlinSymbols.ApolloPrivateEnumConstructor)
            add("$UNKNOWN__($rawValue)\n")
          }
          add("}\n")
        })
        .endControlFlow()
        .build()
  }

  private fun IrEnum.knownValuesFunSpec(): FunSpec {
    return FunSpec.builder(Identifier.knownValues)
        .addKdoc("Returns all [%T] known at build time", selfClassName)
        .addSuppressions(enum.values.any { it.deprecationReason != null })
        .maybeAddOptIn(context.resolver, enum.values)
        .returns(KotlinSymbols.Array.parameterizedBy(selfClassName))
        .addCode(
            CodeBlock.builder()
                .add("return arrayOf(\n")
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

}
