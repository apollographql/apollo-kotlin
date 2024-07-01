/*
 * From https://github.com/ktorio/ktor/blob/3705a229e1e695781b3d97ee069b20d71fb7be67/ktor-client/ktor-client-core/js/src/io/ktor/client/JsUtils.kt
 * Copyright 2014-2023 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package com.apollographql.apollo.network.http

import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get

internal fun <T : JsAny> makeJsObject(): T = js("{ return {}; }")

@Suppress("UNUSED_PARAMETER")
internal fun <T : JsAny> makeJsNew(ctor: JsAny): T = js("new ctor()")

@PublishedApi
@Suppress("UNUSED_PARAMETER")
internal fun <T : JsAny> makeJsCall(func: JsAny, vararg arg: JsAny): T = js("func.apply(null, arg)")

@PublishedApi
@Suppress("UNUSED_PARAMETER")
internal fun makeJsCall(func: JsAny, vararg arg: JsAny): Unit = js("func.apply(null, arg)")

@Suppress("UNUSED_PARAMETER")
internal fun <T : JsAny> makeRequire(name: String): T = js("require(name)")

@Suppress("UNUSED_PARAMETER")
private fun setObjectField(obj: JsAny, name: String, value: JsAny): Unit = js("obj[name]=value")

internal operator fun JsAny.set(name: String, value: JsAny) =
    setObjectField(this, name, value)

internal operator fun JsAny.set(name: String, value: String) =
    setObjectField(this, name, value.toJsString())

internal fun Uint8Array.asByteArray(): ByteArray =
    ByteArray(length) { this[it] }

@Suppress("UNUSED_PARAMETER")
private fun toJsArrayImpl(vararg x: Byte): Uint8Array = js("new Uint8Array(x)")

internal fun ByteArray.asJsArray(): Uint8Array = toJsArrayImpl(*this)
