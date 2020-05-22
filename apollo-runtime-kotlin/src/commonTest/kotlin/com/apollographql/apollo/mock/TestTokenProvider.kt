package com.apollographql.apollo.mock

import com.apollographql.apollo.interceptor.TokenProvider

class TestTokenProvider(currentAccessToken: String,
                        val newAccessToken: String) : TokenProvider {
  var accessToken = currentAccessToken
  override suspend fun currentToken(): String {
    return accessToken
  }

  override suspend fun renewToken(previousToken: String): String {
    accessToken = newAccessToken
    return accessToken
  }
}