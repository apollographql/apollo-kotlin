package com.apollographql.apollo3.compiler.codegen.kotlin.test

import com.apollographql.apollo3.ast.GQLTypeDefinition.Companion.builtInTypes
import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.codegen.Identifier.__map
import com.apollographql.apollo3.compiler.codegen.Identifier.__typename
import com.apollographql.apollo3.compiler.codegen.Identifier.block
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinClassNames
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.codegen.kotlin.adapter.from
import com.apollographql.apollo3.compiler.codegen.kotlin.file.TBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.file.TCtor
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.maybeAddDeprecation
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.maybeAddDescription
import com.apollographql.apollo3.compiler.decapitalizeFirstLetter
import com.apollographql.apollo3.compiler.ir.IrFieldInfo
import com.apollographql.apollo3.compiler.ir.IrInterfaceType
import com.apollographql.apollo3.compiler.ir.IrListType
import com.apollographql.apollo3.compiler.ir.IrNamedType
import com.apollographql.apollo3.compiler.ir.IrNonNullType
import com.apollographql.apollo3.compiler.ir.IrObjectType
import com.apollographql.apollo3.compiler.ir.IrScalarType
import com.apollographql.apollo3.compiler.ir.IrType
import com.apollographql.apollo3.compiler.ir.IrUnionType
import com.apollographql.apollo3.compiler.ir.PossibleTypes
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.MemberName
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
        .superclass(KotlinClassNames.MapBuilder)
        .addProperties(
            tbuilder.properties.map { it.propertySpec(tbuilder.possibleTypes) }
        )
        .addFunctions(
            tbuilder.ctors.map { it.funSpec() }
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
              tbuilder.properties.forEach { fieldInfo ->
                add("%S to %L,\n", fieldInfo.responseName, fieldInfo.resolveCodeBlock() )
              }
            }
            .unindent()
            .add(")\n")
            .build()
    )
    builder.returns(anyMapClassName)
    return builder.build()
  }

  private fun IrType.codeBlock(context: KotlinContext): CodeBlock {
    return when (this) {
      is IrNonNullType -> {
        val notNullFun = MemberName("com.apollographql.apollo3.api", "notNull")
        CodeBlock.of("%L.%M()", ofType.codeBlock(context), notNullFun)
      }
      is IrListType -> {
        val listFun = MemberName("com.apollographql.apollo3.api", "list")
        CodeBlock.of("%L.%M()", ofType.codeBlock(context), listFun)
      }
      is IrNamedType -> {
        context.resolver.resolveCompiledType(name)
      }
      else -> error("Cannot generate compiled type for $this")
    }
  }
  private fun IrFieldInfo.resolveCodeBlock(): CodeBlock {
    if (responseName == "__typename") {
      return CodeBlock.of("__typename")
    }
    return CodeBlock.builder()
        .add("resolve(%S, %L)", responseName, type.codeBlock(context))
        .build()
  }
  private fun TCtor.funSpec(): FunSpec {
    return FunSpec.builder(kotlinName.decapitalizeFirstLetter())
        .addParameter(
            ParameterSpec.builder(
                block,
                LambdaTypeName.get(
                    receiver = context.resolver.resolveTestBuilder(id),
                    parameters = emptyList(),
                    returnType = KotlinClassNames.Unit
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

  private val anyMapClassName = KotlinClassNames.Map.parameterizedBy(KotlinClassNames.String, KotlinClassNames.Any.copy(nullable = true))

  private fun IrFieldInfo.className(): TypeName {
    return context.resolver.resolveIrType(type) {
      when (it) {
        is IrObjectType,
        is IrInterfaceType,
        is IrUnionType,
        -> anyMapClassName
        is IrScalarType -> if (builtInTypes.contains(it.name)) {
          null
        } else {
          KotlinClassNames.String
        }
        else -> null
      }
    }
  }

  private fun IrFieldInfo.propertySpec(possibleTypes: PossibleTypes): PropertySpec {
    if (responseName == "__typename") {
      return if (possibleTypes.size == 1) {
        PropertySpec.builder(__typename, KotlinClassNames.String)
            .initializer("%S", possibleTypes.size)
            .mutable(true)
            .build()
      } else {
        PropertySpec.builder(__typename, KotlinClassNames.String)
            .addKdoc(CodeBlock.of("%L", """
              The typename of this shape isn't known at compile time.
              Possible values: ${possibleTypes.joinToString(", ")}
            """.trimIndent()))
            .delegate(CodeBlock.of("%T()", KotlinClassNames.MandatoryTypenameProperty))
            .mutable(true)
            .build()
      }
    }

    return PropertySpec.builder(context.layout.propertyName(this.responseName), className())
        .mutable(true)
        .maybeAddDescription(description)
        .maybeAddDeprecation(deprecationReason)
        .delegate(CodeBlock.of("%T($__map, %S)", KotlinClassNames.StubbedProperty, responseName))
        .build()
  }
}