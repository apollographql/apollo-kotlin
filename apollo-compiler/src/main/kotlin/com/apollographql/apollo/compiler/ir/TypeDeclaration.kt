package com.apollographql.apollo.compiler.ir

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TypeDeclaration(
    val kind: String,
    val name: String,
    val description: String = "",
    val values: List<TypeDeclarationValue> = emptyList(),
    val fields: List<TypeDeclarationField> = emptyList()
) {
  companion object {
    const val KIND_INPUT_OBJECT_TYPE: String = "InputObjectType"
    const val KIND_ENUM: String = "EnumType"
    const val KIND_SCALAR_TYPE: String = "ScalarType"
    const val ENUM_UNKNOWN_CONSTANT: String = "\$UNKNOWN"
  }
}
