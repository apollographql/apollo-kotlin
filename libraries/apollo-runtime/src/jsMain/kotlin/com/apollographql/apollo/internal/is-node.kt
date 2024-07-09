package com.apollographql.apollo.internal

/**
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 * From https://github.com/ktorio/ktor/blob/6cd529b2dcedfcfc4ca2af0f62704764e160d7fd/ktor-utils/js/src/io/ktor/util/PlatformUtilsJs.kt#L16
 */
internal val isNode: Boolean by lazy {
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
