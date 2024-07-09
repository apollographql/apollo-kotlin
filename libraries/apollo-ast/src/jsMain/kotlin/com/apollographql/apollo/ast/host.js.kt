package com.apollographql.apollo.ast

import okio.FileSystem
import okio.NodeJsFileSystem

internal actual val HOST_FILESYSTEM: FileSystem
  get() = NodeJsFileSystem