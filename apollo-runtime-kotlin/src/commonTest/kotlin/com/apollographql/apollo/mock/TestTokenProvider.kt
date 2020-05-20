package com.apollographql.apollo.mock

import com.apollographql.apollo.interceptor.TokenProvider

class TestTokenProvider(val currentAccessToken: String,
                        val newAccessToken: String) : TokenProvider {
  override suspend fun currentToken(): String {
    return currentAccessToken
  }

  override suspend fun renewToken(): String {
    return newAccessToken
  }
}