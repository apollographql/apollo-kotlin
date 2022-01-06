package com.apollographql.apollo3.compiler.codegen.java.adapter

import com.apollographql.apollo3.api.BooleanExpression
import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.codegen.Identifier.RESPONSE_NAMES
import com.apollographql.apollo3.compiler.codegen.Identifier.__typename
import com.apollographql.apollo3.compiler.codegen.Identifier.customScalarAdapters
import com.apollographql.apollo3.compiler.codegen.Identifier.evaluate
import com.apollographql.apollo3.compiler.codegen.Identifier.fromJson
import com.apollographql.apollo3.compiler.codegen.Identifier.reader
import com.apollographql.apollo3.compiler.codegen.Identifier.toJson
import com.apollographql.apollo3.compiler.codegen.Identifier.typename
import com.apollographql.apollo3.compiler.codegen.Identifier.value
import com.apollographql.apollo3.compiler.codegen.Identifier.writer
import com.apollographql.apollo3.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo3.compiler.codegen.java.JavaContext
import com.apollographql.apollo3.compiler.codegen.java.L
import com.apollographql.apollo3.compiler.codegen.java.S
import com.apollographql.apollo3.compiler.codegen.java.T
import com.apollographql.apollo3.compiler.codegen.java.helpers.codeBlock
import com.apollographql.apollo3.compiler.codegen.java.helpers.toListInitializerCodeblock
import com.apollographql.apollo3.compiler.codegen.java.isNotEmpty
import com.apollographql.apollo3.compiler.codegen.java.joinToCode
import com.apollographql.apollo3.compiler.ir.IrModel
import com.apollographql.apollo3.compiler.ir.IrModelType
import com.apollographql.apollo3.compiler.ir.IrNonNullType
import com.apollographql.apollo3.compiler.ir.IrOptionalType
import com.apollographql.apollo3.compiler.ir.IrProperty
import com.apollographql.apollo3.compiler.ir.IrType
import com.apollographql.apollo3.compiler.ir.isOptional
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import javax.lang.model.element.Modifier

internal fun responseNamesFieldSpec(model: IrModel): FieldSpec {
  val initializer = model.properties.filter { !it.isSynthetic }.map {
    CodeBlock.of(S, it.info.responseName)
  }.toListInitializerCodeblock()

  return FieldSpec.builder(ParameterizedTypeName.get(JavaClassNames.List, JavaClassNames.String), RESPONSE_NAMES)
      .addModifiers(Modifier.FINAL, Modifier.PRIVATE, Modifier.STATIC)
      .initializer(initializer)
      .build()
}

internal fun readFromResponseCodeBlock(
    model: IrModel,
    context: JavaContext,
    hasTypenameArgument: Boolean,
): CodeBlock {
  val (regularProperties, syntheticProperties) = model.properties.partition { !it.isSynthetic }
  val requiresTypename = syntheticProperties.any { it.condition != BooleanExpression.True }

  val prefix = regularProperties.map { property ->
    val variableInitializer = when {
      hasTypenameArgument && property.info.responseName == "__typename" -> CodeBlock.of(typename)
      (property.info.type is IrNonNullType && property.info.type.ofType is IrOptionalType) -> CodeBlock.of("$T", JavaClassNames.Absent)
      else -> CodeBlock.of("null")
    }

    CodeBlock.of(
        "$T $L = $L;",
        context.resolver.resolveIrType(property.info.type),
        context.layout.variableName(property.info.responseName),
        variableInitializer
    )
  }.joinToCode(separator = "\n", suffix = "\n")

  /**
   * Read the regular properties
   */
  val loop = CodeBlock.builder()
      .add("loop:\n")
      .beginControlFlow("while(true)")
      .beginControlFlow("switch ($reader.selectName($RESPONSE_NAMES))")
      .add(
          regularProperties.mapIndexed { index, property ->
            CodeBlock.of(
                "case $L: $L = $L.$fromJson($reader, $customScalarAdapters); break;",
                index,
                context.layout.variableName(property.info.responseName),
                context.resolver.adapterInitializer(property.info.type, property.requiresBuffering)
            )
          }.joinToCode(separator = "\n", suffix = "\n")
      )
      .addStatement("default: break loop")
      .endControlFlow()
      .endControlFlow()
      .build()

  val checkedProperties = mutableSetOf<String>()

  /**
   * Read the synthetic properties
   */
  val checkTypename = if (requiresTypename) {
    checkedProperties.add(__typename)
    CodeBlock.of("$T.checkFieldNotMissing($__typename, $S);", JavaClassNames.Assertions, __typename)
  } else {
    CodeBlock.of("")
  }

  val syntheticLoop = syntheticProperties.map { property ->
    CodeBlock.builder()
        .add("$reader.rewind();\n")
        .apply {
          if (property.condition != BooleanExpression.True) {
            add(
                "$T $L = null;\n",
                context.resolver.resolveIrType(property.info.type),
                context.layout.variableName(property.info.responseName),
            )
            beginControlFlow(
                "if ($T.$evaluate($L, $T.emptySet(), $__typename))",
                JavaClassNames.BooleanExpressions,
                property.condition.codeBlock(),
                JavaClassNames.Collections
            )
          } else {
            checkedProperties.add(property.info.responseName)
            add("$T ", context.resolver.resolveIrType(property.info.type))
          }
        }
        .add(
            CodeBlock.of(
                "$L = $L.INSTANCE.$fromJson($reader, $customScalarAdapters);\n",
                context.layout.variableName(property.info.responseName),
                context.resolver.resolveModelAdapter(property.info.type.modelPath())
            )
        )
        .applyIf(property.condition != BooleanExpression.True) {
          endControlFlow()
        }
        .build()
  }.joinToCode("\n")

  val visibleProperties = model.properties.filter { !it.hidden }

  val checks = CodeBlock.builder()
      .add(
          visibleProperties.filter { property ->
            property.info.type is IrNonNullType
                && !property.info.type.isOptional()
                && !checkedProperties.contains(property.info.responseName)
          }.map { property ->
            CodeBlock.of(
                "$T.checkFieldNotMissing($L, $S);\n",
                JavaClassNames.Assertions,
                context.layout.variableName(property.info.responseName),
                property.info.responseName
            )
          }.joinToCode("")
      ).build()

  val suffix = CodeBlock.builder()
      .add("return new $T(\n", context.resolver.resolveModel(model.id))
      .indent()
      .add(
          visibleProperties.map { property ->
            CodeBlock.of(L, context.layout.variableName(property.info.responseName))
          }.joinToCode(separator = ",\n", suffix = "\n")
      )
      .unindent()
      .add(");\n")
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
      .add(checks)
      .applyIf(checks.isNotEmpty()) { add("\n") }
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

internal fun writeToResponseCodeBlock(model: IrModel, context: JavaContext): CodeBlock {
  return model.properties.filter { !it.hidden }.map { it.writeToResponseCodeBlock(context) }.joinToCode("\n")
}

private fun IrProperty.writeToResponseCodeBlock(context: JavaContext): CodeBlock {
  val builder = CodeBlock.builder()
  val propertyName = context.layout.propertyName(info.responseName)

  if (!isSynthetic) {
    val adapterInitializer = context.resolver.adapterInitializer(info.type, requiresBuffering)
    builder.addStatement("${writer}.name($S)", info.responseName)
    builder.addStatement(
        "$L.$toJson($writer, $customScalarAdapters, $value.$propertyName)",
        adapterInitializer
    )
  } else {
    val adapterInitializer = context.resolver.resolveModelAdapter(info.type.modelPath())

    /**
     * Output types do not distinguish between null and absent
     */
    if (this.info.type !is IrNonNullType) {
      builder.beginControlFlow("if ($value.$propertyName != null)")
    }
    builder.addStatement(
        "$L.INSTANCE.$toJson($writer, $customScalarAdapters, $value.$propertyName)",
        adapterInitializer
    )
    if (this.info.type !is IrNonNullType) {
      builder.endControlFlow()
    }
  }

  return builder.build()
}


internal fun List<String>.toClassName() = ClassName.get(
    first(),
    get(1),
    *drop(2).toTypedArray()
)

fun singletonAdapterInitializer(wrappedTypeName: TypeName, adaptedTypeName: TypeName, buffered: Boolean = false): CodeBlock {
  return CodeBlock.of(
      "new $T($T.INSTANCE, $L)",
      ParameterizedTypeName.get(JavaClassNames.ObjectAdapter, adaptedTypeName),
      wrappedTypeName,
      if (buffered) "true" else "false"
  )
}