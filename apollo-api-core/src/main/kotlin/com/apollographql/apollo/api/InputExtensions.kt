@file:Suppress("NOTHING_TO_INLINE")
@file:JvmName("KotlinExtensions")

package com.apollographql.apollo.api

@JvmSynthetic
inline fun <T : Any> T?.toInput(): Input<T> = Input.optional(this)
