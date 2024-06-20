package com.apollographql.apollo3.testing

import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import com.apollographql.apollo3.annotations.ApolloInternal

@ApolloInternal
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
@Deprecated("This function is not Apollo specific and will be removed in a future version. Copy/paste it in your codebase if you need it")
actual fun currentThreadId(): String {
  return Thread.currentThread().id.toString()
}