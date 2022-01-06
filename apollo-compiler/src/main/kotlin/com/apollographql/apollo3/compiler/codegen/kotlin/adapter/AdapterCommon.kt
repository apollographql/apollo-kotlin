package com.apollographql.apollo3.compiler.codegen.kotlin.adapter

import com.apollographql.apollo3.api.BooleanExpression
import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.codegen.Identifier
import com.apollographql.apollo3.compiler.codegen.Identifier.RESPONSE_NAMES
import com.apollographql.apollo3.compiler.codegen.Identifier.__typename
import com.apollographql.apollo3.compiler.codegen.Identifier.customScalarAdapters
import com.apollographql.apollo3.compiler.codegen.Identifier.fromJson
import com.apollographql.apollo3.compiler.codegen.Identifier.reader
import com.apollographql.apollo3.compiler.codegen.Identifier.typename
import com.apollographql.apollo3.compiler.codegen.Identifier.value
import com.apollographql.apollo3.compiler.codegen.Identifier.writer
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.codeBlock
import com.apollographql.apollo3.compiler.ir.IrModel
import com.apollographql.apollo3.compiler.ir.IrModelType
import com.apollographql.apollo3.compiler.ir.IrNonNullType
import com.apollographql.apollo3.compiler.ir.IrOptionalType
import com.apollographql.apollo3.compiler.ir.IrProperty
import com.apollographql.apollo3.compiler.ir.IrType
import com.apollographql.apollo3.compiler.ir.isOptional
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.joinToCode

internal fun responseNamesPropertySpec(model: IrModel): PropertySpec {
  val initializer = model.properties.filter { !it.isSynthetic }.map {
    CodeBlock.of("%S", it.info.responseName)
  }.joinToCode(prefix = "listOf(", separator = ", ", suffix = ")")

  return PropertySpec.builder(RESPONSE_NAMES, KotlinSymbols.List.parameterizedBy(KotlinSymbols.String))
      .initializer(initializer)
      .build()
}

internal fun readFromResponseCodeBlock(
    model: IrModel,
    context: KotlinContext,
    hasTypenameArgument: Boolean,
): CodeBlock {
  val (regularProperties, syntheticProperties) = model.properties.partition { !it.isSynthetic }
  val requiresTypename = syntheticProperties.any { it.condition != BooleanExpression.True }

  val prefix = regularProperties.map { property ->
    val variableInitializer = when {
      hasTypenameArgument && property.info.responseName == "__typename" -> CodeBlock.of(typename)
      (property.info.type is IrNonNullType && property.info.type.ofType is IrOptionalType) -> CodeBlock.of("%T", KotlinSymbols.Absent)
      else -> CodeBlock.of("null")
    }

    CodeBlock.of(
        "var·%N:·%T·=·%L",
        context.layout.variableName(property.info.responseName),
        context.resolver.resolveIrType(property.info.type).copy(nullable = !property.info.type.isOptional()),
        variableInitializer
    )
  }.joinToCode(separator = "\n", suffix = "\n")

  /**
   * Read the regular properties
   */
  val loop = CodeBlock.builder()
      .beginControlFlow("while(true)")
      .beginControlFlow("when·($reader.selectName($RESPONSE_NAMES))")
      .add(
          regularProperties.mapIndexed { index, property ->
            CodeBlock.of(
                "%L·->·%N·=·%L.$fromJson($reader, $customScalarAdapters)",
                index,
                context.layout.variableName(property.info.responseName),
                context.resolver.adapterInitializer(property.info.type, property.requiresBuffering)
            )
          }.joinToCode(separator = "\n", suffix = "\n")
      )
      .addStatement("else -> break")
      .endControlFlow()
      .endControlFlow()
      .build()

  val checkedProperties = mutableSetOf<String>()

  /**
   * Read the synthetic properties
   */
  val checkTypename = if (requiresTypename) {
    checkedProperties.add(__typename)
    CodeBlock.builder()
        .beginControlFlow("check($__typename·!=·null)")
        .add("%S\n", "__typename was not found")
        .endControlFlow()
        .build()
  } else {
    CodeBlock.of("")
  }

  val syntheticLoop = syntheticProperties.map { property ->
    val evaluate = MemberName("com.apollographql.apollo3.api", "evaluate")
    CodeBlock.builder()
        .add("$reader.rewind()\n")
        .apply {
          if (property.condition != BooleanExpression.True) {
            add(
                "var·%N:·%T·=·null\n",
                context.layout.variableName(property.info.responseName),
                context.resolver.resolveIrType(property.info.type).copy(nullable = !property.info.type.isOptional()),
            )
            beginControlFlow("if·(%L.%M($customScalarAdapters.variables(),·$__typename))", property.condition.codeBlock(), evaluate)
          } else {
            checkedProperties.add(property.info.responseName)
            add("val·")
          }
        }
        .add(
            CodeBlock.of(
                "%L·=·%L.$fromJson($reader, $customScalarAdapters)\n",
                context.layout.variableName(property.info.responseName),
                context.resolver.resolveModelAdapter(property.info.type.modelPath())
            )
        )
        .applyIf(property.condition != BooleanExpression.True) {
          endControlFlow()
        }
        .build()
  }.joinToCode("\n")

  val suffix = CodeBlock.builder()
      .addStatement("return·%T(", context.resolver.resolveModel(model.id))
      .indent()
      .add(model.properties.filter { !it.hidden }.map { property ->
        val maybeAssertNotNull = if (
            property.info.type is IrNonNullType
            && !property.info.type.isOptional()
            && !checkedProperties.contains(property.info.responseName)
        ) {
          "!!"
        } else {
          ""
        }
        CodeBlock.of(
            "%N·=·%N%L",
            context.layout.propertyName(property.info.responseName),
            context.layout.variableName(property.info.responseName),
            maybeAssertNotNull
        )
      }.joinToCode(separator = ",\n", suffix = "\n"))
      .unindent()
      .addStatement(")")
      .build()

  return CodeBlock.builder()
      .add(prefix)
      .applyIf(prefix.isNotEmpty()) { add("\n") }
      .add(loop)
      .applyIf(loop.isNotEmpty()) { add("\n") }
      .add(checkTypename)
      .applyIf(checkTypename.isNotEmpty()) { add("\n") }
      .add(syntheticLoop)
      .applyIf(syntheticLoop.isNotEmpty()) { add("\n") }
      .add(suffix)
      .build()
}

private fun IrType.modelPath(): String {
  return when (this) {
    is IrNonNullType -> ofType.modelPath()
    is IrModelType -> path
    else -> error("Synthetic field has an invalid type: $this")
  }
}

internal fun writeToResponseCodeBlock(model: IrModel, context: KotlinContext): CodeBlock {
  return model.properties.filter { !it.hidden }.map { it.writeToResponseCodeBlock(context) }.joinToCode("\n")
}

private fun IrProperty.writeToResponseCodeBlock(context: KotlinContext): CodeBlock {
  val builder = CodeBlock.builder()
  val propertyName = context.layout.propertyName(info.responseName)

  if (!isSynthetic) {
    val adapterInitializer = context.resolver.adapterInitializer(info.type, requiresBuffering)
    builder.addStatement("${writer}.name(%S)", info.responseName)
    builder.addStatement(
        "%L.${Identifier.toJson}($writer, $customScalarAdapters, $value.%N)",
        adapterInitializer,
        propertyName,
    )
  } else {
    val adapterInitializer = context.resolver.resolveModelAdapter(info.type.modelPath())

    /**
     * Output types do not distinguish between null and absent
     */
    if (this.info.type !is IrNonNullType) {
      builder.beginControlFlow("if·($value.%N·!=·null)", propertyName)
    }
    builder.addStatement(
        "%L.${Identifier.toJson}($writer, $customScalarAdapters, $value.%N)",
        adapterInitializer,
        propertyName,
    )
    if (this.info.type !is IrNonNullType) {
      builder.endControlFlow()
    }
  }

  return builder.build()
}


internal fun ClassName.Companion.from(path: List<String>) = ClassName(
    packageName = path.first(),
    path.drop(1)
)

internal fun CodeBlock.obj(buffered: Boolean): CodeBlock {
  val params = when {
    buffered -> CodeBlock.of("true")
    else -> CodeBlock.of("")
  }
  return CodeBlock.Builder()
      .add("%L", this)
      .add(
          ".%M(%L)",
          MemberName("com.apollographql.apollo3.api", "obj"),
          params
      ).build()
}
