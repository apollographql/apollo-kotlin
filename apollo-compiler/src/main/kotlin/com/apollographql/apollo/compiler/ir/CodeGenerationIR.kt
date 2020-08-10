package com.apollographql.apollo.compiler.ir

import com.squareup.moshi.Moshi

data class CodeGenerationIR(
    val operations: List<Operation>,
    val fragments: List<Fragment>,
    val typesUsed: List<TypeDeclaration>,
    val fragmentsPackageName: String,
    val typesPackageName: String
) {
  fun toJson() = Moshi.Builder().build().adapter(CodeGenerationIR::class.java).toJson(this)

  companion object {
    fun fromJson(json: String) = Moshi.Builder().build().adapter(CodeGenerationIR::class.java).fromJson(json)
  }
}
