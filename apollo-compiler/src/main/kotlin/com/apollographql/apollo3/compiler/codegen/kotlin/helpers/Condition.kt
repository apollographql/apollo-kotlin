package com.apollographql.apollo3.compiler.codegen.kotlin.helpers

import com.apollographql.apollo3.api.BPossibleTypes
import com.apollographql.apollo3.api.BTerm
import com.apollographql.apollo3.api.BVariable
import com.apollographql.apollo3.api.BooleanExpression
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinSymbols
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
          MemberName("com.apollographql.apollo3.api", "and"),
          parameters
      )
    }
    is BooleanExpression.Or -> {
      val parameters = operands.map {
        it.codeBlock()
      }.joinToCode(",")
      CodeBlock.of(
          "%M(%L)",
          MemberName("com.apollographql.apollo3.api", "or"),
          parameters
      )
    }
    is BooleanExpression.Not -> CodeBlock.of(
        "%M(%L)",
        MemberName("com.apollographql.apollo3.api", "not"),
        operand.codeBlock()
    )
    is BooleanExpression.Element -> {
      when (val v = value) {
        is BVariable -> {
          CodeBlock.of(
              "%M(%S)",
              MemberName("com.apollographql.apollo3.api", "variable"),
              v.name
          )
        }
        is BPossibleTypes -> {
          CodeBlock.of(
              "%M(%L)",
              MemberName("com.apollographql.apollo3.api", "possibleTypes"),
              v.possibleTypes.map { CodeBlock.of("%S", it) }.joinToCode(",")
          )
        }
        else -> error("")
      }
    }
    else -> error("")
  }
}