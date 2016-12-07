package com.apollostack.compiler.ir

data class QueryIntermediateRepresentation(
    val operations: List<Operation>,
    val fragments: List<String>,
    val typesUsed: List<TypeDeclaration>
)