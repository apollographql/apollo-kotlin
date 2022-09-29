package com.apollographql.apollo3.api

import okio.FileSystem

internal actual val systemFileSystem: FileSystem
  get() = FileSystem.SYSTEM