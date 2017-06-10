package com.apollographql.apollo.compiler.ir

data class TypeDeclarationValue(
    val name: String,
    val description: String?,
    val isDeprecated: Boolean? = false,
    val deprecationReason: String? = null
)