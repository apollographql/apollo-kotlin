package com.apollographql.apollo.network.http

import com.apollographql.apollo.annotations.ApolloInternal
import io.ktor.client.plugins.HttpTimeout

@ApolloInternal
actual fun HttpTimeout.HttpTimeoutCapabilityConfiguration.setReadTimeout(readTimeoutMillis: Long) {
  // Cannot use socketTimeoutMillis on JS - https://youtrack.jetbrains.com/issue/KTOR-6211
  this.requestTimeoutMillis = readTimeoutMillis
}
