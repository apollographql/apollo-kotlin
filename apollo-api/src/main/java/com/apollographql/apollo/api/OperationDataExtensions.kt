@file:Suppress("NOTHING_TO_INLINE")
@file:JvmMultifileClass
@file:JvmName("KotlinExtensions")

package com.apollographql.apollo.api

import com.apollographql.apollo.response.ScalarTypeAdapters

@JvmSynthetic
inline fun toJson(data: Operation.Data, indent: String, scalarTypeAdapters: ScalarTypeAdapters = ScalarTypeAdapters.DEFAULT): String =
    OperationDataJsonSerializer.serialize(data, indent, scalarTypeAdapters)
