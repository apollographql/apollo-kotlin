package com.apollographql.apollo3.api

import okio.FileSystem

internal actual val systemFileSystem: FileSystem
  get() = throw IllegalStateException("There is no SYSTEM filesystem on JS")