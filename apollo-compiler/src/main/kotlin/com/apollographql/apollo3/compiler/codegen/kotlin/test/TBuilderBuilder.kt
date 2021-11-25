package com.apollographql.apollo3.compiler.codegen.kotlin.test

import com.apollographql.apollo3.ast.GQLTypeDefinition.Companion.builtInTypes
import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.codegen.Identifier.__map
import com.apollographql.apollo3.compiler.codegen.Identifier.__typename
import com.apollographql.apollo3.compiler.codegen.Identifier.block
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.codegen.kotlin.adapter.from
import com.apollographql.apollo3.compiler.codegen.kotlin.file.TBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.file.TCtor
import com.apollographql.apollo3.compiler.codegen.kotlin.file.TProperty
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.maybeAddDeprecation
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.maybeAddDescription
import com.apollographql.apollo3.compiler.codegen.kotlin.selections.CompiledSelectionsBuilder.Companion.codeBlock
import com.apollographql.apollo3.compiler.ir.IrEnumType
import com.apollographql.apollo3.compiler.ir.IrModelType
import com.apollographql.apollo3.compiler.ir.IrScalarType
import com.apollographql.apollo3.compiler.ir.PossibleTypes
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec

internal class TBuilderBuilder(
    private val context: KotlinContext,
    private val tbuilder: TBuilder,
    private val path: List<String>,
    private val inner: Boolean,
) {
  private val builderName = tbuilder.kotlinName

  private val nestedBuilders = tbuilder.nestedTBuilders.map {
    TBuilderBuilder(
        context,
        it,
        path + builderName,
        true
    )
  }

  fun prepare() {
    context.resolver.registerTestBuilder(
        tbuilder.id,
        ClassName.from(path + builderName)
    )
    nestedBuilders.forEach { it.prepare() }
  }

  fun build(): TypeSpec {
    return TypeSpec.classBuilder(builderName)
        .applyIf(inner) {
          addModifiers(KModifier.INNER)
        }
        .superclass(KotlinSymbols.MapBuilder)
        .addProperties(
            tbuilder.properties.map { it.propertySpec(tbuilder.possibleTypes) }
        )
        .addFunctions(
            tbuilder.properties.flatMap { it.ctors.map { it.funSpec() } }
        )
        .addFunction(
            buildFun()
        )
        .addTypes(
            nestedBuilders.map { it.build() }
        )
        .build()
  }

  private fun buildFun(): FunSpec {
    val builder = FunSpec.builder("build")
    builder.addModifiers(KModifier.OVERRIDE)
    builder.addCode(
        CodeBlock.builder()
            .add("return mapOf(\n")
            .indent()
            .apply {
              tbuilder.properties.forEach { tprop ->
                add("%S to %L,\n", tprop.responseName, tprop.resolveCodeBlock())
              }
            }
            .unindent()
            .add(")\n")
            .build()
    )
    builder.returns(anyMapClassName)
    return builder.build()
  }

  private fun TProperty.resolveCodeBlock(): CodeBlock {
    if (responseName == "__typename") {
      return CodeBlock.of("__typename")
    }
    return CodeBlock.builder()
        .add("resolve(%S, %L", responseName, gqlType!!.codeBlock(context))
        .apply {
          ctors.forEach {
            add(", { %L() }", it.kotlinName)
          }
        }
        .add(")")
        .build()
  }

  private fun TCtor.funSpec(): FunSpec {
    return FunSpec.builder(kotlinName)
        .addParameter(
            ParameterSpec.builder(
                block,
                LambdaTypeName.get(
                    receiver = context.resolver.resolveTestBuilder(id),
                    parameters = emptyList(),
                    returnType = KotlinSymbols.Unit
                )
            )
                .defaultValue(CodeBlock.of("{}"))
                .build()
        )
        .addCode(
            CodeBlock.of("return %T().apply($block).build()", context.resolver.resolveTestBuilder(id))
        )
        .returns(anyMapClassName)
        .build()
  }

  private val anyMapClassName = KotlinSymbols.Map.parameterizedBy(KotlinSymbols.String, KotlinSymbols.Any.copy(nullable = true))

  private fun TProperty.className(): TypeName {
    return context.resolver.resolveIrType(type) {
      when (it) {
        is IrEnumType -> KotlinSymbols.String
        is IrModelType -> anyMapClassName
        is IrScalarType -> if (builtInTypes.contains(it.name)) {
          null
        } else {
          KotlinSymbols.String
        }
        else -> null
      }?.copy(nullable = true)
    }
  }

  private fun TProperty.propertySpec(possibleTypes: PossibleTypes): PropertySpec {
    if (responseName == "__typename") {
      return if (possibleTypes.size == 1) {
        PropertySpec.builder(__typename, KotlinSymbols.String)
            .initializer("%S", possibleTypes.single())
            .mutable(true)
            .build()
      } else {
        PropertySpec.builder(__typename, KotlinSymbols.String)
            .addKdoc(CodeBlock.of("%L", """
              The typename of this shape isn't known at compile time.
              Possible values: ${possibleTypes.joinToString(", ")}
            """.trimIndent()))
            .delegate(CodeBlock.of("%T()", KotlinSymbols.MandatoryTypenameProperty))
            .mutable(true)
            .build()
      }
    }

    return PropertySpec.builder(context.layout.propertyName(this.responseName), className())
        .mutable(true)
        .maybeAddDescription(description)
        .maybeAddDeprecation(deprecationReason)
        .delegate(CodeBlock.of("%T($__map, %S)", KotlinSymbols.StubbedProperty, responseName))
        .build()
  }
}