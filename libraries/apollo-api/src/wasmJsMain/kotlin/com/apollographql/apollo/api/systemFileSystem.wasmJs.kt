package com.apollographql.apollo.api

import okio.FileSystem

internal actual val systemFileSystem: FileSystem
  get() = throw IllegalStateException("There is no SYSTEM filesystem on wasmJs")