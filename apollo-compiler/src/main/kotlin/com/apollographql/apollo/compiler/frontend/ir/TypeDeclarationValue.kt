package com.apollographql.apollo.compiler.frontend.ir

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TypeDeclarationValue(
    val name: String,
    val description: String = "",
    val isDeprecated: Boolean = false,
    val deprecationReason: String = ""
)
