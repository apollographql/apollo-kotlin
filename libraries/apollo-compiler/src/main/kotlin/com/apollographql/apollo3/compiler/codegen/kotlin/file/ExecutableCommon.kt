package com.apollographql.apollo3.compiler.codegen.kotlin.file

import com.apollographql.apollo3.api.Executable
import com.apollographql.apollo3.compiler.codegen.Identifier
import com.apollographql.apollo3.compiler.codegen.Identifier.customScalarAdapters
import com.apollographql.apollo3.compiler.codegen.Identifier.root
import com.apollographql.apollo3.compiler.codegen.Identifier.rootField
import com.apollographql.apollo3.compiler.codegen.Identifier.selections
import com.apollographql.apollo3.compiler.codegen.Identifier.serializeVariables
import com.apollographql.apollo3.compiler.codegen.Identifier.toJson
import com.apollographql.apollo3.compiler.codegen.Identifier.writer
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinResolver
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.patchKotlinNativeOptionalArrayProperties
import com.apollographql.apollo3.compiler.ir.IrBooleanValue
import com.apollographql.apollo3.compiler.ir.IrProperty
import com.apollographql.apollo3.compiler.ir.IrVariable
import com.apollographql.apollo3.compiler.ir.isOptional
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec

internal fun serializeVariablesFunSpec(
    adapterClassName: TypeName?,
    emptyMessage: String,
): FunSpec {

  val body = if (adapterClassName == null) {
    CodeBlock.of("""
      // $emptyMessage
    """.trimIndent())
  } else {
    CodeBlock.of(
        "%L.$toJson($writer, $customScalarAdapters, this)",
        CodeBlock.of("%T", adapterClassName)
    )
  }
  return FunSpec.builder(serializeVariables)
      .addModifiers(KModifier.OVERRIDE)
      .addParameter(writer, KotlinSymbols.JsonWriter)
      .addParameter(customScalarAdapters, KotlinSymbols.CustomScalarAdapters)
      .addCode(body)
      .build()
}

internal fun adapterFunSpec(
    resolver: KotlinResolver,
    property: IrProperty,
): FunSpec {
  val type = property.info.type

  return FunSpec.builder("adapter")
      .addModifiers(KModifier.OVERRIDE)
      .returns(KotlinSymbols.Adapter.parameterizedBy(resolver.resolveIrType(type)))
      .addCode(
          CodeBlock.of(
              "return·%L",
              resolver.adapterInitializer(type, property.requiresBuffering)
          )
      )
      .build()
}

internal fun rootFieldFunSpec(context: KotlinContext, parentType: String, selectionsClassName: ClassName): FunSpec {
  return FunSpec.builder(rootField)
      .addModifiers(KModifier.OVERRIDE)
      .returns(KotlinSymbols.CompiledField)
      .addCode(
          CodeBlock.builder()
              .add("return·%T(\n", KotlinSymbols.CompiledFieldBuilder)
              .indent()
              .add("name·=·%S,\n", Identifier.data)
              .add("type·=·%L\n", context.resolver.resolveCompiledType(parentType))
              .unindent()
              .add(")\n")
              .add(".$selections(selections·=·%T.$root)\n", selectionsClassName)
              .add(".build()\n")
              .build()
      )
      .build()
}

internal fun TypeSpec.maybeAddFilterNotNull(generateFilterNotNull: Boolean): TypeSpec {
  if (!generateFilterNotNull) {
    return this
  }
  return patchKotlinNativeOptionalArrayProperties()
}

internal fun variablesFunSpec(variables: List<IrVariable>): FunSpec {
  val code = CodeBlock.builder()
      .apply {
        addStatement("val variables = %L", "mutableMapOf<String, Any?>()")
        for (variable in variables) {
          if (variable.type.isOptional()) {
            if (variable.defaultValue is IrBooleanValue) {
              beginControlFlow("when (%N) {", variable.name)

              beginControlFlow("is %T ->", KotlinSymbols.Present)
              addStatement("variables[%S] = %N.value", variable.name, variable.name)
              endControlFlow()

              beginControlFlow("is %T ->", KotlinSymbols.Absent)

              beginControlFlow("if (%N)", Identifier.withDefaultBooleanValues)
              addStatement("variables[%S] = %L", variable.name, variable.defaultValue.value)
              endControlFlow()

              endControlFlow()

              endControlFlow()
            } else {
              beginControlFlow("if (%N is %T)", variable.name, KotlinSymbols.Present)
              addStatement("variables[%S] = %N.value", variable.name, variable.name)
              endControlFlow()
            }
          } else {
            addStatement("variables[%S] = %N", variable.name, variable.name)
          }
        }
        addStatement("return %T(%L)", Executable.Variables::class.java, "variables")
      }
      .build()
  return FunSpec.builder(Identifier.variables)
      .addModifiers(KModifier.OVERRIDE)
      .addParameter(Identifier.withDefaultBooleanValues, KotlinSymbols.Boolean)
      .returns(Executable.Variables::class.java)
      .addCode(code)
      .build()
}
