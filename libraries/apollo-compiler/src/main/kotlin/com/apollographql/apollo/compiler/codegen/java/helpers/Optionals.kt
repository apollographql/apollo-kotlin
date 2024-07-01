package com.apollographql.apollo.compiler.codegen.java.helpers

import com.apollographql.apollo.compiler.JavaNullable
import com.apollographql.apollo.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo.compiler.codegen.java.JavaContext
import com.apollographql.apollo.compiler.codegen.java.L
import com.apollographql.apollo.compiler.codegen.java.T
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.TypeName

internal fun JavaContext.wrapValueInOptional(value: CodeBlock, fieldType: TypeName): CodeBlock {
  return if (!resolver.isOptional(fieldType)) {
    CodeBlock.of(L, value)
  } else {
    when (nullableFieldStyle) {
      JavaNullable.JAVA_OPTIONAL -> CodeBlock.of("$T.of($L)", JavaClassNames.JavaOptional, value)
      JavaNullable.GUAVA_OPTIONAL -> CodeBlock.of("$T.of($L)", JavaClassNames.GuavaOptional, value)
      else -> CodeBlock.of("$T.present($L)", JavaClassNames.Optional, value)
    }
  }
}

internal fun JavaContext.unwrapOptionalValue(value: CodeBlock, fieldType: TypeName): CodeBlock {
  return if (!resolver.isOptional(fieldType)) {
    CodeBlock.of(L, value)
  } else {
    when (nullableFieldStyle) {
      JavaNullable.JAVA_OPTIONAL -> CodeBlock.of("$L.get()", value)
      JavaNullable.GUAVA_OPTIONAL -> CodeBlock.of("$L.get()", value)
      else -> CodeBlock.of("$L.getOrThrow()", value)
    }
  }
}

internal fun JavaContext.testOptionalValuePresence(value: CodeBlock, fieldType: TypeName): CodeBlock {
  return if (!resolver.isOptional(fieldType)) {
    CodeBlock.of("$L != null", value)
  } else {
    when (nullableFieldStyle) {
      JavaNullable.JAVA_OPTIONAL -> CodeBlock.of("$L.isPresent()", value)
      JavaNullable.GUAVA_OPTIONAL -> CodeBlock.of("$L.isPresent()", value)
      else -> CodeBlock.of("$L instanceof $T", value, JavaClassNames.Present)
    }
  }
}

internal fun JavaContext.absentOptionalInitializer(): CodeBlock {
  return when (nullableFieldStyle) {
    JavaNullable.JAVA_OPTIONAL -> CodeBlock.of("$T.empty()", JavaClassNames.JavaOptional)
    JavaNullable.GUAVA_OPTIONAL -> CodeBlock.of("$T.absent()", JavaClassNames.GuavaOptional)
    else -> CodeBlock.of("$T.absent()", JavaClassNames.Optional)
  }
}

internal fun JavaContext.absentOptionalInitializer(fieldType: TypeName): CodeBlock {
  return if (!resolver.isOptional(fieldType)) {
    CodeBlock.of("null")
  } else {
    absentOptionalInitializer()
  }
}
