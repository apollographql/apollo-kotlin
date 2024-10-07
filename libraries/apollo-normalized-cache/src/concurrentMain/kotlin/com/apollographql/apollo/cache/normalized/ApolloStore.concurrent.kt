package com.apollographql.apollo.cache.normalized

import kotlin.reflect.KClass

internal actual fun KClass<*>.normalizedCacheName(): String {
  return qualifiedName ?: toString()
}
