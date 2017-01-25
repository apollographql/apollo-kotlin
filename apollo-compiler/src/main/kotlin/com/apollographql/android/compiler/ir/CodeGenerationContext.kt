package com.apollographql.android.compiler.ir

data class CodeGenerationContext(
    val abstractType: Boolean,
    val reservedTypeNames: List<String>,
    val typeDeclarations: List<TypeDeclaration>,
    val fragmentsPackage: String = "",
    val typesPackage: String = "",
    val customTypeMap: Map<String, String>
)