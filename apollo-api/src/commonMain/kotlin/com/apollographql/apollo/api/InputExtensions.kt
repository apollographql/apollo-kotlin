@file:Suppress("NOTHING_TO_INLINE")
@file:JvmName("KotlinExtensions")
@file:JvmMultifileClass

package com.apollographql.apollo.api

import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName
import kotlin.jvm.JvmSynthetic

@JvmSynthetic
inline fun <T : Any> T?.toInput(): Input<T> = Input.optional(this)
