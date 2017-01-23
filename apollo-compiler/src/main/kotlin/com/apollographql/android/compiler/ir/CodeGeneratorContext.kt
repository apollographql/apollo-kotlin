package com.apollographql.android.compiler.ir

data class CodeGeneratorContext(
    val abstractType: Boolean,
    val reservedTypeNames: List<String>,
    val typeDeclarations: List<TypeDeclaration>,
    val fragmentsPackage: String = "",
    val typesPackage: String = "",
    val customScalarTypeMap: Map<String, String>
)