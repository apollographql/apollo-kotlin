package com.apollographql.apollo3.network.http

import io.ktor.client.plugins.HttpTimeout

actual fun HttpTimeout.HttpTimeoutCapabilityConfiguration.setReadTimeout(readTimeoutMillis: Long) {
  // Cannot use socketTimeoutMillis on JS - https://youtrack.jetbrains.com/issue/KTOR-6211
  this.requestTimeoutMillis = readTimeoutMillis
}
