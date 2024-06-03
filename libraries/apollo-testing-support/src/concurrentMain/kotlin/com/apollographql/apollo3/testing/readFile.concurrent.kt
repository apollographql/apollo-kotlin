package com.apollographql.apollo3.testing

import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import com.apollographql.apollo3.annotations.ApolloExperimental
import okio.FileSystem
import okio.SYSTEM

/**
 * The host filesystem
 */
@ApolloExperimental
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
@Deprecated("This function is not Apollo specific and will be removed in a future version. Copy/paste it in your codebase if you need it")
actual val HostFileSystem: FileSystem
  get() = FileSystem.SYSTEM