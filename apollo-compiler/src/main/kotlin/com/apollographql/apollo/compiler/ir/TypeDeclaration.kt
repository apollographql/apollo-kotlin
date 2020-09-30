package com.apollographql.apollo.compiler.ir

data class TypeDeclaration(
    val kind: String,
    val name: String,
    val description: String = "",
    val values: List<TypeDeclarationValue> = emptyList(),
    val fields: List<TypeDeclarationField> = emptyList()
) {
  companion object {
    val KIND_INPUT_OBJECT_TYPE: String = "InputObjectType"
    val KIND_ENUM: String = "EnumType"
    val KIND_SCALAR_TYPE: String = "ScalarType"
    val ENUM_UNKNOWN_CONSTANT: String = "\$UNKNOWN"
    val ENUM_SAFE_VALUE_OF: String = "safeValueOf"
  }
}