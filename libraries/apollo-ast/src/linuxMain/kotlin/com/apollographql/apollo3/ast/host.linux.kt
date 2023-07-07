package com.apollographql.apollo3.ast

import okio.FileSystem

internal actual val HOST_FILESYSTEM: FileSystem
  get() = FileSystem.SYSTEM