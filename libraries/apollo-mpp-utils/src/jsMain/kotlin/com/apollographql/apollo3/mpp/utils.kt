package com.apollographql.apollo3.mpp

import com.apollographql.apollo3.annotations.ApolloInternal
import kotlin.js.Date

actual fun currentTimeMillis(): Long {
  return Date().getTime().toLong()
}

actual fun currentTimeFormatted(): String {
  return Date().toISOString()
}

actual fun currentThreadId(): String {
  return "js"
}

actual fun currentThreadName(): String {
  return currentThreadId()
}

actual fun platform() = Platform.Js

/**
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 * From https://github.com/ktorio/ktor/blob/6cd529b2dcedfcfc4ca2af0f62704764e160d7fd/ktor-utils/js/src/io/ktor/util/PlatformUtilsJs.kt#L16
 */
@ApolloInternal
val isNode: Boolean by lazy {
  js(
      """
      (typeof process !== 'undefined' 
          && process.versions != null 
          && process.versions.node != null) ||
      (typeof window !== 'undefined' 
          && typeof window.process !== 'undefined' 
          && window.process.versions != null 
          && window.process.versions.node != null)
    """
  ) as Boolean
}
