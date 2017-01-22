package com.apollostack.compiler.ir

data class OperationIntermediateRepresentation(
    val operations: List<Operation>,
    val fragments: List<Fragment>,
    val typesUsed: List<TypeDeclaration>
)
