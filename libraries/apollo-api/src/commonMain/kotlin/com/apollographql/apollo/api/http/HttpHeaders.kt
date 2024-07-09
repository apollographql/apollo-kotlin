@file:JvmName("HttpHeaders")
package com.apollographql.apollo.api.http

import kotlin.jvm.JvmName

/**
 * Get the value of the "name" header. HTTP header names are case insensitive, see https://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2
 *
 * @param name: the name of the header
 */
fun List<HttpHeader>.valueOf(name: String): String? = firstOrNull { it.name.equals(name, true) }?.value
