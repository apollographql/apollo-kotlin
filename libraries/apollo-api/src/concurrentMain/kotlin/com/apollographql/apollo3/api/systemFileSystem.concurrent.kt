package com.apollographql.apollo3.api

import okio.FileSystem
import okio.SYSTEM

internal actual val systemFileSystem: FileSystem
  get() = FileSystem.SYSTEM