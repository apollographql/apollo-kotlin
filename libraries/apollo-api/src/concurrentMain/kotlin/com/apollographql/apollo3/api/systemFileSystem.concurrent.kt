package com.apollographql.apollo.api

import okio.FileSystem
import okio.SYSTEM

internal actual val systemFileSystem: FileSystem
  get() = FileSystem.SYSTEM