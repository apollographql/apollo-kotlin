package com.apollographql.android.compiler.ir

import com.apollographql.android.compiler.NullableValueType

data class CodeGenerationContext(
    var reservedTypeNames: List<String>,
    val typeDeclarations: List<TypeDeclaration>,
    val fragmentsPackage: String = "",
    val typesPackage: String = "",
    val customTypeMap: Map<String, String>,
    val nullableValueType: NullableValueType,
    val generateAccessors: Boolean
)