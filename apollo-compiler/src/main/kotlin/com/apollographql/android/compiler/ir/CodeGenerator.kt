package com.apollographql.android.compiler.ir

import com.squareup.javapoet.TypeSpec

interface CodeGenerator {
  fun toTypeSpec(context: CodeGenerationContext): TypeSpec
}
