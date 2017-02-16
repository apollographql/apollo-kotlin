package com.apollographql.android.compiler.ir

data class CodeGenerationIR(
    val operations: List<Operation>,
    val fragments: List<Fragment>,
    val typesUsed: List<TypeDeclaration>
)
