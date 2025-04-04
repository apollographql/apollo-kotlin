package com.apollographql.apollo.testing

import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import com.apollographql.apollo.annotations.ApolloExperimental
import okio.FileSystem
import okio.SYSTEM

/**
 * The host filesystem
 */
actual val HostFileSystem: FileSystem
  get() = FileSystem.SYSTEM