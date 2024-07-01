package com.apollographql.apollo.mpp

import com.apollographql.apollo.annotations.ApolloInternal
import kotlin.js.Date

actual fun currentTimeMillis(): Long {
  return Date().getTime().toLong()
}

