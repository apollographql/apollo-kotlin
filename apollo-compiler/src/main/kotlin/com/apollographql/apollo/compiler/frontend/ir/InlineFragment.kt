package com.apollographql.apollo.compiler.frontend.ir

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class InlineFragment(
    val typeCondition: String,
    val possibleTypes: List<String> = emptyList(),
    val description: String,
    val fields: List<Field>,
    val inlineFragments: List<InlineFragment>,
    val fragments: List<FragmentRef>,
    val sourceLocation: SourceLocation,
    val conditions: List<Condition> = emptyList()
)