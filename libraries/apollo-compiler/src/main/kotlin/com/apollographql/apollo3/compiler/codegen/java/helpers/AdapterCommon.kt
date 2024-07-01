package com.apollographql.apollo.compiler.codegen.java.helpers

import com.apollographql.apollo.compiler.codegen.Identifier
import com.apollographql.apollo.compiler.codegen.Identifier.RESPONSE_NAMES
import com.apollographql.apollo.compiler.codegen.Identifier.__path
import com.apollographql.apollo.compiler.codegen.Identifier.__typename
import com.apollographql.apollo.compiler.codegen.Identifier.customScalarAdapters
import com.apollographql.apollo.compiler.codegen.Identifier.evaluate
import com.apollographql.apollo.compiler.codegen.Identifier.fromJson
import com.apollographql.apollo.compiler.codegen.Identifier.reader
import com.apollographql.apollo.compiler.codegen.Identifier.toJson
import com.apollographql.apollo.compiler.codegen.Identifier.typename
import com.apollographql.apollo.compiler.codegen.Identifier.value
import com.apollographql.apollo.compiler.codegen.Identifier.writer
import com.apollographql.apollo.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo.compiler.codegen.java.JavaContext
import com.apollographql.apollo.compiler.codegen.java.L
import com.apollographql.apollo.compiler.codegen.java.S
import com.apollographql.apollo.compiler.codegen.java.T
import com.apollographql.apollo.compiler.codegen.java.isNotEmpty
import com.apollographql.apollo.compiler.codegen.java.javaPropertyName
import com.apollographql.apollo.compiler.codegen.java.joinToCode
import com.apollographql.apollo.compiler.codegen.variableName
import com.apollographql.apollo.compiler.internal.applyIf
import com.apollographql.apollo.compiler.ir.BLabel
import com.apollographql.apollo.compiler.ir.BooleanExpression
import com.apollographql.apollo.compiler.ir.IrModel
import com.apollographql.apollo.compiler.ir.IrModelType
import com.apollographql.apollo.compiler.ir.IrProperty
import com.apollographql.apollo.compiler.ir.IrType
import com.apollographql.apollo.compiler.ir.firstElementOfType
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import javax.lang.model.element.Modifier

internal fun responseNamesFieldSpec(model: IrModel): FieldSpec? {
  val regularProperties = model.properties.filter { !it.isSynthetic }
  if (regularProperties.isEmpty()) {
    return null
  }

  val initializer = regularProperties.map {
    CodeBlock.of(S, it.info.responseName)
  }.toListInitializerCodeblock()

  return FieldSpec.builder(ParameterizedTypeName.get(JavaClassNames.List, JavaClassNames.String), RESPONSE_NAMES)
      .addModifiers(Modifier.FINAL, Modifier.PRIVATE, Modifier.STATIC)
      .initializer(initializer)
      .build()
}

private fun javaTypenameFromReaderCodeBlock(): CodeBlock {
  return CodeBlock.builder()
      .add("String $__typename = $T.readTypename($reader);\n", JavaClassNames.JsonReaders)
      .build()
}

internal fun readFromResponseCodeBlock(
    model: IrModel,
    context: JavaContext,
    hasTypenameArgument: Boolean,
): CodeBlock {
  val (regularProperties, syntheticProperties) = model.properties.partition { !it.isSynthetic }
  val prefix = regularProperties.map { property ->
    val resolvedType = context.resolver.resolveIrType(property.info.type).withoutAnnotations()
    val variableInitializer = when {
      hasTypenameArgument && property.info.responseName == "__typename" -> CodeBlock.of(typename)
      property.info.type.optional -> CodeBlock.of(T, JavaClassNames.Absent)
      resolvedType == TypeName.INT -> CodeBlock.of("0")
      resolvedType == TypeName.DOUBLE -> CodeBlock.of("0.0")
      resolvedType == TypeName.BOOLEAN -> CodeBlock.of("false")
      else -> CodeBlock.of("null")
    }

    CodeBlock.of(
        "$T $L = $L;",
        resolvedType,
        property.info.responseName.variableName(),
        variableInitializer
    )
  }.joinToCode(separator = "\n", suffix = "\n")

  val path = if (syntheticProperties.any { it.condition.firstElementOfType(BLabel::class) != null }) {
    CodeBlock.of("List<Object> $__path = $reader.${Identifier.getPath}();")
  } else {
    CodeBlock.of("")
  }

  /**
   * Read the regular properties
   */
  val loop = if (regularProperties.isNotEmpty()) {
    CodeBlock.builder()
        .add("loop:\n")
        .beginControlFlow("while(true)")
        .beginControlFlow("switch ($reader.selectName($RESPONSE_NAMES))")
        .add(
            regularProperties.mapIndexed { index, property ->
              val variableName = property.info.responseName.variableName()
              val adapterInitializer = context.resolver.adapterInitializer(property.info.type, property.requiresBuffering)

              CodeBlock.of(
                  "case $L: $L = $L.$fromJson($reader, $customScalarAdapters); break;",
                  index,
                  variableName,
                  adapterInitializer,
              )
            }.joinToCode(separator = "\n", suffix = "\n")
        )
        .addStatement("default: break loop")
        .endControlFlow()
        .endControlFlow()
        .build()
  } else {
    CodeBlock.of("")
  }

  val checkedProperties = mutableSetOf<String>()

  /**
   * Read the synthetic properties
   */
  val typenameCodeBlock = if (syntheticProperties.any { it.requiresTypename }) {
    checkedProperties.add(__typename)

    CodeBlock.builder()
        .apply {
          if (regularProperties.none { it.info.responseName == "__typename" }) {
            // We are in a nested fragment that needs access to __typename, get it from the buffered reader
            add("$reader.rewind();\n")
            add(javaTypenameFromReaderCodeBlock())
          } else {
            add("$T.checkFieldNotMissing($__typename, $S);", JavaClassNames.Assertions, __typename)
          }
        }.build()
  } else {
    CodeBlock.of("")
  }

  val syntheticLoop = syntheticProperties.map { property ->
    val fromJsonCall = CodeBlock.of(
        "$L.INSTANCE.$fromJson($reader, $customScalarAdapters)",
        context.resolver.resolveModelAdapter(property.info.type.modelPath())
    )
    val resolvedType = context.resolver.resolveIrType(property.info.type).withoutAnnotations()
    CodeBlock.builder()
        .apply {
          if (property.condition != BooleanExpression.True) {
            add(
                "$T $L = $L;\n",
                resolvedType,
                property.info.responseName.variableName(),
                context.absentOptionalInitializer(resolvedType)
            )
            val pathLiteral = if (path.isNotEmpty()) {
              __path
            } else {
              "null"
            }
            beginControlFlow(
                "if ($T.$evaluate($L, $customScalarAdapters.falseVariables, $__typename, $customScalarAdapters.deferredFragmentIdentifiers, $pathLiteral))",
                JavaClassNames.BooleanExpressions,
                property.condition.codeBlock(),
            )
            add("$reader.rewind();\n")
          } else {
            checkedProperties.add(property.info.responseName)
            add("$reader.rewind();\n")
            add("$T ", resolvedType)
          }
        }
        .add(
            CodeBlock.of(
                "$L = $L;\n",
                property.info.responseName.variableName(),
                context.wrapValueInOptional(fromJsonCall, resolvedType)
            )
        )
        .applyIf(property.condition != BooleanExpression.True) {
          endControlFlow()
        }
        .build()
  }.joinToCode("\n")

  val visibleProperties = model.properties

  val checks = CodeBlock.builder()
      .add(
          visibleProperties.filter { property ->
            !property.info.type.nullable
                && !property.info.type.optional
                && !checkedProperties.contains(property.info.responseName)
          }.map { property ->
            CodeBlock.of(
                "$T.checkFieldNotMissing($L, $S);\n",
                JavaClassNames.Assertions,
                property.info.responseName.variableName(),
                property.info.responseName
            )
          }.joinToCode("")
      ).build()

  val suffix = CodeBlock.builder()
      .add("return new $T(\n", context.resolver.resolveModel(model.id))
      .indent()
      .add(
          visibleProperties.map { property ->
            CodeBlock.of(L, property.info.responseName.variableName())
          }.joinToCode(separator = ",\n", suffix = "\n")
      )
      .unindent()
      .add(");\n")
      .build()

  return CodeBlock.builder()
      .add(prefix)
      .applyIf(prefix.isNotEmpty()) { add("\n") }
      .add(path)
      .applyIf(path.isNotEmpty()) { add("\n") }
      .add(loop)
      .applyIf(loop.isNotEmpty()) { add("\n") }
      .add(typenameCodeBlock)
      .applyIf(typenameCodeBlock.isNotEmpty()) { add("\n") }
      .add(syntheticLoop)
      .applyIf(syntheticLoop.isNotEmpty()) { add("\n") }
      .add(checks)
      .applyIf(checks.isNotEmpty()) { add("\n") }
      .add(suffix)
      .build()
}

private fun IrType.modelPath(): String {
  return when (this) {
    is IrModelType -> path
    else -> error("Synthetic field has an invalid type: $this")
  }
}

internal fun writeToResponseCodeBlock(model: IrModel, context: JavaContext): CodeBlock {
  return model.properties.map { it.writeToResponseCodeBlock(context) }.joinToCode("\n")
}

private fun IrProperty.writeToResponseCodeBlock(context: JavaContext): CodeBlock {
  val builder = CodeBlock.builder()
  val propertyName = context.layout.javaPropertyName(info.responseName)

  if (!isSynthetic) {
    val adapterInitializer = context.resolver.adapterInitializer(info.type, requiresBuffering)
    builder.addStatement("${writer}.name($S)", info.responseName)
    builder.addStatement(
        "$L.$toJson($writer, $customScalarAdapters, $value.$propertyName)",
        adapterInitializer,
    )
  } else {
    val adapterInitializer = context.resolver.resolveModelAdapter(info.type.modelPath())

    /**
     * Output types do not distinguish between null and absent
     */
    val resolvedType = context.resolver.resolveIrType(info.type).withoutAnnotations()
    if (this.info.type.nullable) {
      val property = CodeBlock.of("$value.$propertyName")
      val propertyTest = context.testOptionalValuePresence(property, resolvedType)
      builder.beginControlFlow("if ($L)", propertyTest)
    }
    val fieldValue = CodeBlock.of("$value.$propertyName")
    builder.addStatement(
        "$L.INSTANCE.$toJson($writer, $customScalarAdapters, $L)",
        adapterInitializer,
        context.unwrapOptionalValue(fieldValue, resolvedType)
    )
    if (this.info.type.nullable) {
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
