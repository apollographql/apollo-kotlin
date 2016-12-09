package com.apollostack.compiler.ir

import com.squareup.javapoet.TypeSpec

interface CodeGenerator {
  fun toTypeSpec(fragments: List<Fragment>): TypeSpec
}