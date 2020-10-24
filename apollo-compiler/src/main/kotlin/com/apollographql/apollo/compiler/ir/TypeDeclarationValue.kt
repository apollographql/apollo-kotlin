package com.apollographql.apollo.compiler.ir

data class TypeDeclarationValue(
    val name: String,
    val description: String = "",
    val deprecationReason: String? = null
)
