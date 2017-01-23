package com.apollographql.android.compiler.ir

data class OperationIntermediateRepresentation(
    val operations: List<Operation>,
    val fragments: List<Fragment>,
    val typesUsed: List<TypeDeclaration>
)
