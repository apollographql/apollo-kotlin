package com.apollographql.apollo.api.internal.json

actual fun String.Companion.format(format: String, vararg args: Any?): String = String.format(format, *args)
