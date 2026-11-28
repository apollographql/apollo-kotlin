package com.apollographql.apollo.compiler.codegen.kotlin.helpers

import com.apollographql.apollo.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo.compiler.ir.BLabel
import com.apollographql.apollo.compiler.ir.BPossibleTypes
import com.apollographql.apollo.compiler.ir.BTerm
import com.apollographql.apollo.compiler.ir.BVariable
import com.apollographql.apollo.compiler.ir.BooleanExpression
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.joinToCode

internal fun BooleanExpression<BTerm>.codeBlock(): CodeBlock {
  return when (this) {
    is BooleanExpression.False -> CodeBlock.of("%T", KotlinSymbols.False)
    is BooleanExpression.True -> CodeBlock.of("%T", KotlinSymbols.True)
    is BooleanExpression.And -> {
      val parameters = operands.map {
        it.codeBlock()
      }.joinToCode(",")

      CodeBlock.of(
          "%M(%L)",
          MemberName("com.apollographql.apollo.api", "and"),
          parameters
      )
    }
    is BooleanExpression.Or -> {
      val parameters = operands.map {
        it.codeBlock()
      }.joinToCode(",")
      CodeBlock.of(
          "%M(%L)",
          MemberName("com.apollographql.apollo.api", "or"),
          parameters
      )
    }
    is BooleanExpression.Not -> CodeBlock.of(
        "%M(%L)",
        MemberName("com.apollographql.apollo.api", "not"),
        operand.codeBlock()
    )
    is BooleanExpression.Element -> {
      when (val v = value) {
        is BVariable -> {
          CodeBlock.of(
              "%M(%S)",
              MemberName("com.apollographql.apollo.api", "variable"),
              v.name
          )
        }
        is BLabel -> {
          if (v.label == null) {
            CodeBlock.of("%M()", MemberName("com.apollographql.apollo.api", "label"))
          } else {
            CodeBlock.of(
                "%M(%S)",
                MemberName("com.apollographql.apollo.api", "label"),
                v.label
            )
          }
        }
        is BPossibleTypes -> {
          CodeBlock.of(
              "%M(%L)",
              MemberName("com.apollographql.apollo.api", "possibleTypes"),
              v.possibleTypes.map { CodeBlock.of("%S", it) }.joinToCode(",")
          )
        }
      }
    }
  }
}

/**
 * Collects the conditions used by an adapter's `fromJson` so that they can be hoisted to private properties of the
 * adapter object and built only once instead of on every call.
 *
 * - A condition that is only a type condition becomes a `Set<String>` that is checked with `__typename in set`.
 * - Any other condition becomes a `BooleanExpression<BTerm>` on which `evaluate()` is called.
 *
 * Identical conditions are deduplicated within an adapter.
 */
internal class HoistedConditions {
  private val possibleTypes = mutableMapOf<Set<String>, PropertySpec>()
  private val expressions = mutableMapOf<BooleanExpression<BTerm>, PropertySpec>()

  /**
   * Returns the name of a `Set<String>` property containing [types]
   */
  fun possibleTypes(types: Set<String>): String {
    return possibleTypes.getOrPut(types) {
      PropertySpec.builder("__possibleTypes${possibleTypes.size}", KotlinSymbols.Set.parameterizedBy(KotlinSymbols.String))
          .addModifiers(KModifier.PRIVATE)
          .initializer("setOf(%L)", types.map { CodeBlock.of("%S", it) }.joinToCode(", "))
          .build()
    }.name
  }

  /**
   * Returns the name of a `BooleanExpression<BTerm>` property containing [expression]
   */
  fun expression(expression: BooleanExpression<BTerm>): String {
    return expressions.getOrPut(expression) {
      PropertySpec.builder("__condition${expressions.size}", KotlinSymbols.BooleanExpression.parameterizedBy(KotlinSymbols.BTerm))
          .addModifiers(KModifier.PRIVATE)
          .initializer(expression.codeBlock())
          .build()
    }.name
  }

  fun propertySpecs(): List<PropertySpec> = possibleTypes.values + expressions.values
}
