package com.apollographql.apollo3.compiler.codegen.kotlin.executableschema

import com.apollographql.apollo3.compiler.capitalizeFirstLetter
import com.apollographql.apollo3.compiler.codegen.executionPackageName
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinExecutableSchemaContext
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.addSuppressions
import com.apollographql.apollo3.compiler.internal.applyIf
import com.apollographql.apollo3.compiler.ir.IrExecutionContextTargetArgument
import com.apollographql.apollo3.compiler.ir.IrGraphqlTargetArgument
import com.apollographql.apollo3.compiler.ir.IrTargetArgument
import com.apollographql.apollo3.compiler.ir.IrTargetField
import com.apollographql.apollo3.compiler.ir.IrTargetObject
import com.apollographql.apollo3.compiler.ir.asKotlinPoet
import com.apollographql.apollo3.compiler.ir.optional
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.joinToCode

internal class MainResolverBuilder(
    val context: KotlinExecutableSchemaContext,
    val serviceName: String,
    val irTargetObjects: List<IrTargetObject>,
) : CgFileBuilder {
  private val packageName = context.layout.executionPackageName()
  private val simpleName = "${serviceName}Resolver".capitalizeFirstLetter()

  val className = ClassName(packageName, simpleName)

  override fun prepare() {}

  override fun build(): CgFile {
    return CgFile(
        packageName = packageName,
        typeSpecs = listOf(typeSpec()),
        fileName = simpleName
    )
  }


  private fun typeSpec(): TypeSpec {
    return TypeSpec.objectBuilder(simpleName)
        .addSuperinterface(ClassName("com.apollographql.apollo3.execution", "MainResolver"))
        .addProperty(typenamesPropertySpec())
        .addProperty(resolversPropertySpec())
        .addFunction(typenameFunSpec())
        .addFunction(resolveFunSpec())
        .addSuppressions(deprecation = true)
        .build()
  }

  private fun typenameFunSpec(): FunSpec {
    return FunSpec.builder("typename")
        .addModifiers(KModifier.OVERRIDE)
        .returns(KotlinSymbols.String.copy(nullable = true))
        .addParameter(ParameterSpec("obj", KotlinSymbols.Any))
        .addCode("return typenames[obj::class]")
        .build()
  }

  private fun resolveFunSpec(): FunSpec {
    return FunSpec.builder("resolve")
        .addModifiers(KModifier.OVERRIDE)
        .returns(KotlinSymbols.Any.copy(nullable = true))
        .addParameter(ParameterSpec("resolveInfo", KotlinSymbols.ResolveInfo))
        .addCode(CodeBlock.builder()
            .add("val·ret·=·resolvers.get(resolveInfo.parentType)?.get(resolveInfo.fieldName)?.resolve(resolveInfo)\n")
            .add("if (ret == null) {\n")
            .indent()
            .add("error(\"Unsupported·field·'${'$'}{resolveInfo.parentType}.${'$'}{resolveInfo.fieldName}'\")\n")
            .unindent()
            .add("}\n")
            .add("return ret\n")
            .build()
        )
        .build()
  }

  private fun typenamesPropertySpec(): PropertySpec {
    return PropertySpec.builder(
        "typenames",
        KotlinSymbols.Map.parameterizedBy(
            ClassName("kotlin.reflect", "KClass").parameterizedBy(STAR),
            KotlinSymbols.String
        )
    ).addModifiers(KModifier.PRIVATE)
        .initializer(
            CodeBlock.Builder()
                .add("mapOf(\n")
                .indent()
                .apply {
                  irTargetObjects.forEach {
                    add("%T::class·to·%S,\n", it.targetClassName.asKotlinPoet(), it.name)
                  }
                }
                .unindent()
                .add(")\n")
                .build()
        ).build()
  }

  private fun resolversPropertySpec(): PropertySpec {
    return PropertySpec.builder(
        "resolvers",
        KotlinSymbols.Map.parameterizedBy(
            KotlinSymbols.String,
            KotlinSymbols.Map.parameterizedBy(
                KotlinSymbols.String,
                KotlinSymbols.Resolver
            )
        )
    ).addModifiers(KModifier.PRIVATE)
        .initializer(
            CodeBlock.Builder()
                .add("mapOf(\n")
                .indent()
                .apply {
                  irTargetObjects.forEach { irTargetObject ->
                    add("%S·to·mapOf(\n", irTargetObject.name)
                    indent()
                    irTargetObject.fields.forEach { irTargetField ->
                      add("%S·to·%T·%L,\n", irTargetField.name, KotlinSymbols.Resolver, resolverBody(irTargetObject, irTargetField))
                    }
                    unindent()
                    add("),\n")
                  }
                }
                .unindent()
                .add(")\n")
                .build()
        ).build()
  }

  private fun resolverBody(irTargetObject: IrTargetObject, irTargetField: IrTargetField): CodeBlock {
    val singleLine = irTargetField.arguments.size < 2

    return CodeBlock.Builder()
        .apply {
          if ((!singleLine)) {
            add("{\n")
            indent()
          } else {
            add("{·")
          }
        }
        .add("(it.parentObject·as·%T).%L", irTargetObject.targetClassName.asKotlinPoet(), irTargetField.targetName)
        .applyIf(irTargetField.isFunction) {
          if (singleLine) {
            add("(")
            add(
                irTargetField.arguments.map { irTargetArgument ->
                  argumentCodeBlock(irTargetArgument)
                }.joinToCode(",·")
            )
            add(")")
          } else {
            add("(\n")
            indent()
            add(
                irTargetField.arguments.map { irTargetArgument ->
                  argumentCodeBlock(irTargetArgument)
                }.joinToCode(",\n", suffix = "\n")
            )
            unindent()
            add(")\n")
          }
        }
        .apply {
          if (!singleLine) {
            unindent()
            add("}")
          } else {
            add("·}")
          }
        }

        .build()
  }

  private fun argumentCodeBlock(irTargetArgument: IrTargetArgument): CodeBlock {
    val builder = CodeBlock.builder()
    when (irTargetArgument) {
      is IrGraphqlTargetArgument -> {
        /**
         * Unwrap the optional because getArgument always return an optional value
         */
        val type = if (irTargetArgument.type.optional) {
          irTargetArgument.type.optional(false)
        } else {
          irTargetArgument.type
        }
        builder.add(
            "%L·=·it.getArgument<%T>(%S)",
            irTargetArgument.targetName,
            context.resolver.resolveIrType(type = type, jsExport = false, isInterface = false),
            irTargetArgument.name,
        )
        if (!irTargetArgument.type.optional) {
          builder.add(".getOrThrow()")
        }
      }

      IrExecutionContextTargetArgument -> {
        builder.add("executionContext·=·it.executionContext")
      }
    }
    return builder.build()
  }
}