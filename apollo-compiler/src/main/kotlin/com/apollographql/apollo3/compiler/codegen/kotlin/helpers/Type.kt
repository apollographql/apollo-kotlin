package com.apollographql.apollo3.compiler.codegen.kotlin.helpers

import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.ir.IrListType
import com.apollographql.apollo3.compiler.ir.IrModelType
import com.apollographql.apollo3.compiler.ir.IrNamedType
import com.apollographql.apollo3.compiler.ir.IrNonNullType
import com.apollographql.apollo3.compiler.ir.IrOptionalType
import com.apollographql.apollo3.compiler.ir.IrType
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName

internal fun IrType.codeBlock(context: KotlinContext): CodeBlock {
  return when (this) {
    is IrNonNullType -> {
      val notNullFun = MemberName("com.apollographql.apollo3.api", "notNull")
      CodeBlock.of("%L.%M()", ofType.codeBlock(context), notNullFun)
    }
    is IrListType -> {
      val listFun = MemberName("com.apollographql.apollo3.api", "list")
      CodeBlock.of("%L.%M()", ofType.codeBlock(context), listFun)
    }
    is IrNamedType -> {
      context.resolver.resolveCompiledType(name)
    }
    is IrModelType -> TODO()
    is IrOptionalType -> TODO()
  }
}
