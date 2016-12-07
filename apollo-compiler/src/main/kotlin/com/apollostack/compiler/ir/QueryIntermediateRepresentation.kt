package com.apollostack.compiler.ir

data class QueryIntermediateRepresentation(
    val operations: List<Operation>,
    val fragments: List<Fragment>,
    val typesUsed: List<TypeDeclaration>
)