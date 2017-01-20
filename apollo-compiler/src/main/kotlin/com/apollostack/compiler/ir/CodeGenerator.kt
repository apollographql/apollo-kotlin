package com.apollostack.compiler.ir

import com.squareup.javapoet.TypeSpec

interface CodeGenerator {
  fun toTypeSpec(abstract: Boolean, reservedTypeNames: List<String>, typeDeclarations: List<TypeDeclaration>): TypeSpec
}