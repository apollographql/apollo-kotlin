package com.apollographql.apollo3.network.http

import com.apollographql.apollo3.annotations.ApolloInternal
import io.ktor.client.plugins.HttpTimeout

@ApolloInternal
actual fun HttpTimeout.HttpTimeoutCapabilityConfiguration.setReadTimeout(readTimeoutMillis: Long) {
  this.socketTimeoutMillis = readTimeoutMillis
}
