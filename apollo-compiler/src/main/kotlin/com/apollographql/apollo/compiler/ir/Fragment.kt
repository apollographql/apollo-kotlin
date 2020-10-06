package com.apollographql.apollo.compiler.ir

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Fragment(
    val fragmentName: String,
    val source: String,
    val description: String,
    val typeCondition: String,
    val possibleTypes: List<String>,
    val fields: List<Field>,
    val fragmentRefs: List<FragmentRef>,
    val inlineFragments: List<InlineFragment>,
    val filePath: String,
    val sourceLocation: SourceLocation
)