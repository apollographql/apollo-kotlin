package com.apollographql.apollo3.compiler.codegen.java.helpers

import com.apollographql.apollo3.api.BPossibleTypes
import com.apollographql.apollo3.api.BTerm
import com.apollographql.apollo3.api.BVariable
import com.apollographql.apollo3.api.BooleanExpression
import com.apollographql.apollo3.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo3.compiler.codegen.java.L
import com.apollographql.apollo3.compiler.codegen.java.S
import com.apollographql.apollo3.compiler.codegen.java.T
import com.apollographql.apollo3.compiler.codegen.java.joinToCode
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.ParameterizedTypeName

internal fun BooleanExpression<BTerm>.codeBlock(): CodeBlock {
  return when(this) {
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
      val params = when(val v = value) {
        is BVariable -> {
          CodeBlock.of(
              "new $T($S)",
              JavaClassNames.BVariable,
              v.name
          )
        }
        is BPossibleTypes -> {
          CodeBlock.of(
              "new $T($L)",
              JavaClassNames.BPossibleTypes,
              v.possibleTypes.map { CodeBlock.of(S, it) }.joinToCode(",")
          )
        }
        else -> error("")
      }

      CodeBlock.of("new $T($L)", ParameterizedTypeName.get(JavaClassNames.BooleanExpressionElement, JavaClassNames.BTerm), params)
    }
    else -> error("")
  }
}