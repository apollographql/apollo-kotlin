package com.apollographql.apollo.compiler.ir

import com.squareup.moshi.JsonClass

data class TypeDeclarationField(
    val name: String,
    val description: String = "",
    val type: String,
    val defaultValue: Any?
)
