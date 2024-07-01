package com.apollographql.apollo.compiler.codegen.kotlin.helpers

import com.apollographql.apollo.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo.compiler.ir.BLabel
import com.apollographql.apollo.compiler.ir.BPossibleTypes
import com.apollographql.apollo.compiler.ir.BTerm
import com.apollographql.apollo.compiler.ir.BVariable
import com.apollographql.apollo.compiler.ir.BooleanExpression
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
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
