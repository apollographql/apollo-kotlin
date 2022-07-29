package com.apollographql.apollo3.compiler.codegen.kotlin.helpers

import com.apollographql.apollo3.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo3.compiler.codegen.java.L
import com.apollographql.apollo3.compiler.codegen.java.S
import com.apollographql.apollo3.compiler.codegen.java.T
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.joinToCode


fun List<CodeBlock>.toListInitializerCodeblock(withNewLines: Boolean = false): CodeBlock {
  if (isEmpty()) {
    return CodeBlock.of("emptyList()")
  }

  return if (withNewLines) {
    CodeBlock.builder()
        .add("listOf(\n")
        .indent()
        .add("%L", joinToCode(",\n"))
        .unindent()
        .add("\n)")
        .build()
  } else {
    CodeBlock.builder()
        .add("listOf(")
        .add("%L", joinToCode(", "))
        .add(")")
        .build()
  }
}

fun List<Pair<String, CodeBlock>>.toMapInitializerCodeblock(withNewLines: Boolean = false): CodeBlock {
  if (isEmpty()) {
    return CodeBlock.of("emptyMap()")
  }

  val items = map { CodeBlock.of("%S to %L", it.first, it.second) }
  return if (withNewLines) {
    CodeBlock.builder()
        .add("mapOf(\n")
        .indent()
        .add("%L", items.joinToCode(",\n"))
        .unindent()
        .add("\n)")
        .build()
  } else {
    CodeBlock.builder()
        .add("mapOf(")
        .add("%L", items.joinToCode(", "))
        .add(")")
        .build()
  }
}
