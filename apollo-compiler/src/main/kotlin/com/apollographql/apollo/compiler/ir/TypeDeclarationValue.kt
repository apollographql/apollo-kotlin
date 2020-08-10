package com.apollographql.apollo.compiler.ir

import com.squareup.moshi.JsonClass

data class TypeDeclarationValue(
    val name: String,
    val description: String = "",
    val isDeprecated: Boolean = false,
    val deprecationReason: String = ""
)
