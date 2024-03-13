package com.apollographql.apollo3.api

import okio.FileSystem
import okio.NodeJsFileSystem

internal actual val systemFileSystem: FileSystem
  get() = NodeJsFileSystem