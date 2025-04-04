package com.apollographql.apollo.testing

import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import com.apollographql.apollo.annotations.ApolloInternal

actual fun currentThreadId(): String {
  return Thread.currentThread().id.toString()
}