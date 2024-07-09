package com.apollographql.apollo.network.http

import com.apollographql.apollo.annotations.ApolloInternal
import io.ktor.client.plugins.HttpTimeout

@ApolloInternal
actual fun HttpTimeout.HttpTimeoutCapabilityConfiguration.setReadTimeout(readTimeoutMillis: Long) {
  this.socketTimeoutMillis = readTimeoutMillis
}
