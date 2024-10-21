package com.apollographql.apollo.network.http

import com.apollographql.apollo.annotations.ApolloInternal
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpTimeoutConfig

@ApolloInternal
actual fun HttpTimeoutConfig.setReadTimeout(readTimeoutMillis: Long) {
  this.socketTimeoutMillis = readTimeoutMillis
}
