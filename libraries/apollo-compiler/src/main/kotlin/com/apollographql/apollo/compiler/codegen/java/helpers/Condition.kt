package com.apollographql.apollo.compiler.codegen.java.helpers

import com.apollographql.apollo.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo.compiler.codegen.java.L
import com.apollographql.apollo.compiler.codegen.java.S
import com.apollographql.apollo.compiler.codegen.java.T
import com.apollographql.apollo.compiler.codegen.java.joinToCode
import com.apollographql.apollo.compiler.ir.BLabel
import com.apollographql.apollo.compiler.ir.BPossibleTypes
import com.apollographql.apollo.compiler.ir.BTerm
import com.apollographql.apollo.compiler.ir.BVariable
import com.apollographql.apollo.compiler.ir.BooleanExpression
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.ParameterizedTypeName
import javax.lang.model.element.Modifier

internal fun BooleanExpression<BTerm>.codeBlock(): CodeBlock {
  return when (this) {
    is BooleanExpression.False -> CodeBlock.of("$T.INSTANCE", JavaClassNames.False)
    is BooleanExpression.True -> CodeBlock.of("$T.INSTANCE", JavaClassNames.True)
    is BooleanExpression.And -> {
      val parameters = operands.map {
        it.codeBlock()
      }.joinToCode(",")

      CodeBlock.of(
          "new $T($L)",
          ParameterizedTypeName.get(JavaClassNames.And, JavaClassNames.BTerm),
          parameters
      )
    }
    is BooleanExpression.Or -> {
      val parameters = operands.map {
        it.codeBlock()
      }.joinToCode(",")
      CodeBlock.of(
          "new $T($L)",
          ParameterizedTypeName.get(JavaClassNames.Or, JavaClassNames.BTerm),
          parameters
      )
    }
    is BooleanExpression.Not -> CodeBlock.of(
        "new $T($L)",
        ParameterizedTypeName.get(JavaClassNames.Not, JavaClassNames.BTerm),
        operand.codeBlock()
    )
    is BooleanExpression.Element -> {
      val params = when (val v = value) {
        is BVariable -> {
          CodeBlock.of(
              "new $T($S)",
              JavaClassNames.BVariable,
              v.name
          )
        }
        is BLabel -> {
          CodeBlock.of(
              "new $T($S)",
              JavaClassNames.BLabel,
              v.label
          )
        }
        is BPossibleTypes -> {
          CodeBlock.of(
              "new $T($L)",
              JavaClassNames.BPossibleTypes,
              v.possibleTypes.map { CodeBlock.of(S, it) }.joinToCode(",")
          )
        }
      }

      CodeBlock.of("new $T($L)", ParameterizedTypeName.get(JavaClassNames.BooleanExpressionElement, JavaClassNames.BTerm), params)
    }
  }
}

/**
 * Collects the conditions used by an adapter's `fromJson` so that they can be hoisted to static fields of the
 * adapter and built only once instead of on every call.
 *
 * - A condition that is only a type condition becomes a `Set<String>` that is checked with `set.contains(__typename)`.
 * - Any other condition becomes a `BooleanExpression<BTerm>` that is passed to `evaluate()`.
 *
 * Identical conditions are deduplicated within an adapter.
 */
internal class HoistedConditions {
  private val possibleTypes = mutableMapOf<Set<String>, FieldSpec>()
  private val expressions = mutableMapOf<BooleanExpression<BTerm>, FieldSpec>()

  /**
   * Returns the name of a `Set<String>` field containing [types]
   */
  fun possibleTypes(types: Set<String>): String {
    return possibleTypes.getOrPut(types) {
      FieldSpec.builder(ParameterizedTypeName.get(JavaClassNames.Set, JavaClassNames.String), "__possibleTypes${possibleTypes.size}")
          .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
          .initializer("new $T<>($L)", JavaClassNames.HashSet, types.map { CodeBlock.of(S, it) }.toListInitializerCodeblock())
          .build()
    }.name
  }

  /**
   * Returns the name of a `BooleanExpression<BTerm>` field containing [expression]
   */
  fun expression(expression: BooleanExpression<BTerm>): String {
    return expressions.getOrPut(expression) {
      FieldSpec.builder(ParameterizedTypeName.get(JavaClassNames.BooleanExpression, JavaClassNames.BTerm), "__condition${expressions.size}")
          .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
          .initializer(expression.codeBlock())
          .build()
    }.name
  }

  fun fieldSpecs(): List<FieldSpec> = possibleTypes.values + expressions.values
}
