package com.apollographql.android.compiler.ir

import com.apollographql.android.compiler.NullableValueGenerationType

data class CodeGenerationContext(
    val reservedTypeNames: List<String>,
    val typeDeclarations: List<TypeDeclaration>,
    val fragmentsPackage: String = "",
    val typesPackage: String = "",
    val customTypeMap: Map<String, String>,
    val nullableValueGenerationType: NullableValueGenerationType
)