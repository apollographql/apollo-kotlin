package com.apollographql.apollo.compiler.ir

import com.squareup.moshi.Moshi

data class CodeGenerationIR(
    val operations: List<Operation>,
    val fragments: List<Fragment>,
    val typesUsed: List<TypeDeclaration>,
    val fragmentsPackageName: String,
    val typesPackageName: String
)