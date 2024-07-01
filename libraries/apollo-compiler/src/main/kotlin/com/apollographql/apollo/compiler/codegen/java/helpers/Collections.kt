package com.apollographql.apollo.compiler.codegen.java.helpers

import com.apollographql.apollo.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo.compiler.codegen.java.L
import com.apollographql.apollo.compiler.codegen.java.S
import com.apollographql.apollo.compiler.codegen.java.T
import com.apollographql.apollo.compiler.codegen.java.joinToCode
import com.squareup.javapoet.CodeBlock

internal fun List<CodeBlock>.toListInitializerCodeblock(withNewLines: Boolean = false): CodeBlock {
  if (isEmpty()) {
    return CodeBlock.of("$T.emptyList()", JavaClassNames.Collections)
  }

  val newLine = if (withNewLines) "\n" else ""
  val space = if (withNewLines) "" else " "
  return CodeBlock.builder()
      .add("$T.asList($newLine", JavaClassNames.Arrays)
      .indent()
      .add(L, joinToCode(",$newLine$space"))
      .unindent()
      .add("$newLine)")
      .build()
}

internal fun List<CodeBlock>.toArrayInitializerCodeblock(): CodeBlock {
  return CodeBlock.builder()
      .add("{")
      .add(L, joinToCode(", "))
      .add("}")
      .build()
}

internal fun List<Pair<String, CodeBlock>>.toMapInitializerCodeblock(): CodeBlock {
  if (isEmpty()) {
    return CodeBlock.of("$T.emptyMap()", JavaClassNames.Collections)
  }
  return CodeBlock.builder()
      .add("new $T()", JavaClassNames.ImmutableMapBuilder)
      .apply {
        forEach {
          add(".put($S, $L)", it.first, it.second)
        }
        add(".build()")
      }
      .build()
}