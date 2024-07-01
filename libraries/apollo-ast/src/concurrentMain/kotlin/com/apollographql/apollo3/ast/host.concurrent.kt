package com.apollographql.apollo.ast

import okio.FileSystem
import okio.SYSTEM

internal actual val HOST_FILESYSTEM: FileSystem
  get() = FileSystem.SYSTEM