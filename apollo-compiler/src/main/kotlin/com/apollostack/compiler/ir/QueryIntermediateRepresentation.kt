package com.apollostack.compiler.ir

class QueryIntermediateRepresentation(val operations: List<Operation>,
    val fragments: List<String>, val typesUsed: List<String>) {
}