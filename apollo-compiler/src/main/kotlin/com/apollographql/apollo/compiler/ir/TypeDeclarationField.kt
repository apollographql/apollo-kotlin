package com.apollographql.apollo.compiler.ir

data class TypeDeclarationField(
    val name: String,
    val description: String,
    val type: String,
    val defaultValue: Any?
)