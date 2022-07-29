package com.apollographql.apollo3.compiler.codegen.java.helpers

import com.apollographql.apollo3.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo3.compiler.codegen.java.JavaContext
import com.apollographql.apollo3.compiler.codegen.java.L
import com.apollographql.apollo3.compiler.codegen.java.T
import com.apollographql.apollo3.compiler.ir.IrListType
import com.apollographql.apollo3.compiler.ir.IrModelType
import com.apollographql.apollo3.compiler.ir.IrNamedType
import com.apollographql.apollo3.compiler.ir.IrNonNullType
import com.apollographql.apollo3.compiler.ir.IrOptionalType
import com.apollographql.apollo3.compiler.ir.IrType
import com.squareup.javapoet.CodeBlock

internal fun IrType.codeBlock(context: JavaContext): CodeBlock {
  return when (this) {
    is IrNonNullType -> {
      CodeBlock.of("new $T($L)", JavaClassNames.CompiledNotNullType, ofType.codeBlock(context))
    }
    is IrListType -> {
      CodeBlock.of("new $T($L)", JavaClassNames.CompiledListType, ofType.codeBlock(context))
    }
    is IrNamedType -> {
      context.resolver.resolveCompiledType(name)
    }
    is IrModelType -> TODO()
    is IrOptionalType -> TODO()
  }
}