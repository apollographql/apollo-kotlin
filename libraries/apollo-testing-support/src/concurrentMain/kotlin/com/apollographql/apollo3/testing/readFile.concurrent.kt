package com.apollographql.apollo3.testing

import com.apollographql.apollo3.annotations.ApolloExperimental
import okio.FileSystem
import okio.SYSTEM

/**
 * The host filesystem
 */
@ApolloExperimental
actual val HostFileSystem: FileSystem
  get() = FileSystem.SYSTEM