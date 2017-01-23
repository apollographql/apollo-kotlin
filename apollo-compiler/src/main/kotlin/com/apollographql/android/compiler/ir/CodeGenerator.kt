package com.apollographql.android.compiler.ir

import com.squareup.javapoet.TypeSpec

//TODO needs refactoring
interface CodeGenerator {
  fun toTypeSpec(abstractClass: Boolean, reservedTypeNames: List<String>, typeDeclarations: List<TypeDeclaration>,
      fragmentsPackage: String = "", typesPackage: String = "", customScalarTypeMap: Map<String, String>): TypeSpec
}
