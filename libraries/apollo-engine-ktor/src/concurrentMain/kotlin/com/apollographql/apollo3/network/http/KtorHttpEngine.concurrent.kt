package com.apollographql.apollo3.network.http

import io.ktor.client.plugins.HttpTimeout

actual fun HttpTimeout.HttpTimeoutCapabilityConfiguration.setReadTimeout(readTimeoutMillis: Long) {
  this.socketTimeoutMillis = readTimeoutMillis
}
