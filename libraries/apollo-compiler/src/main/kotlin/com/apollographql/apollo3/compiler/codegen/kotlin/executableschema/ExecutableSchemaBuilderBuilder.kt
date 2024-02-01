package com.apollographql.apollo3.compiler.codegen.kotlin.executableschema

import com.apollographql.apollo3.compiler.capitalizeFirstLetter
import com.apollographql.apollo3.compiler.codegen.executionPackageName
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinExecutableSchemaContext
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo3.compiler.ir.IrTargetObject
import com.apollographql.apollo3.compiler.ir.asKotlinPoet
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.joinToCode

internal class ExecutableSchemaBuilderBuilder(
    private val context: KotlinExecutableSchemaContext,
    private val serviceName: String,
    private val mainResolver: ClassName,
    private val adapterRegistry: MemberName,
    private val irTargetObjects: List<IrTargetObject>,
) : CgFileBuilder {
  val simpleName = "${serviceName}ExecutableSchemaBuilder".capitalizeFirstLetter()
  override fun prepare() {

  }

  override fun build(): CgFile {
    return CgFile(packageName = context.layout.executionPackageName(), fileName = simpleName, funSpecs = listOf(funSpec())
    )
  }

  private fun funSpec(): FunSpec {
    val rootIrTargetObjects = listOf("query", "mutation", "subscription").map { operationType ->
      irTargetObjects.find { it.operationType == operationType }
    }

    return FunSpec.builder(simpleName)
        .returns(KotlinSymbols.ExecutableSchemaBuilder)
        .apply {
          addParameter(ParameterSpec.builder("schema", KotlinSymbols.Schema).build())
          rootIrTargetObjects.forEach { irTargetObject ->
            if (irTargetObject != null) {
              addParameter(
                  ParameterSpec.builder(
                      name = "root${irTargetObject.operationType?.capitalizeFirstLetter()}Object",
                      type = LambdaTypeName.get(parameters = emptyList(), returnType = irTargetObject.targetClassName.asKotlinPoet())
                  ).apply {
                    if (irTargetObject.isSingleton) {
                      defaultValue(CodeBlock.of("{ %L }", irTargetObject.targetClassName.asKotlinPoet()))
                    } else if (irTargetObject.hasNoArgsConstructor) {
                      defaultValue(CodeBlock.of("{ %L() }", irTargetObject.targetClassName.asKotlinPoet()))
                    }
                  }.build())
            }
          }
        }
        .addCode(
            CodeBlock.builder()
                .add("return %L()\n", KotlinSymbols.ExecutableSchemaBuilder)
                .add(".schema(schema)\n")
                .add(".resolver(%L)\n", mainResolver)
                .add(".adapterRegistry(%L)\n", adapterRegistry)
                .add(".roots(%L.create(", KotlinSymbols.Roots)
                .apply {
                  rootIrTargetObjects.map { irTargetObject ->
                    if (irTargetObject == null) {
                      CodeBlock.of("null")
                    } else {
                      CodeBlock.of("root${irTargetObject.operationType?.capitalizeFirstLetter()}Object")
                    }
                  }.joinToCode(", ")
                      .let {
                        add(it)
                      }
                }
                .add("))\n")
                .build()
        )
        .build()
  }

}