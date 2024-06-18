package com.apollographql.apollo3.mpp

import com.apollographql.apollo3.annotations.ApolloInternal
import kotlin.js.Date

actual fun currentTimeMillis(): Long {
  return Date().getTime().toLong()
}

