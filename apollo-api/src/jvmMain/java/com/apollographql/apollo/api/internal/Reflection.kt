package com.apollographql.apollo.api.internal

import kotlin.reflect.KClass

actual val KClass<*>.qualifiedName2: String?
  get() = qualifiedName