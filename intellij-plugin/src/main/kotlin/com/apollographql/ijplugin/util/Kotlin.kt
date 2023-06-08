package com.apollographql.ijplugin.util

inline fun <reified T> Any?.cast(): T? = this as? T
