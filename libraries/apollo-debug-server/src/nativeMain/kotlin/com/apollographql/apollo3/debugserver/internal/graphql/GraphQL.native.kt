package com.apollographql.apollo3.debugserver.internal.graphql


import kotlin.reflect.KClass

internal actual fun getExecutableSchema(): String = throw UnsupportedOperationException("Not supported on this platform")

internal actual fun KClass<*>.normalizedCacheName(): String = qualifiedName ?: toString()
