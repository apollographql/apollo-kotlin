package com.apollographql.apollo.compiler.ir

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi

@JsonClass(generateAdapter = true)
data class CodeGenerationIR(
    val operations: List<Operation>,
    val fragments: List<Fragment>,
    val typesUsed: List<TypeDeclaration>
) {
  fun toJson() = Moshi.Builder().build().adapter(CodeGenerationIR::class.java).toJson(this)

  companion object {
    fun fromJson(json: String) = Moshi.Builder().build().adapter(CodeGenerationIR::class.java).fromJson(json)
  }
}
