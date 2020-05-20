package com.apollographql.apollo.mock

import com.apollographql.apollo.interceptor.AccessTokenProvider

class TestAccessTokenProvider(val currentAccessToken: String,
                              val newAccessToken: String) : AccessTokenProvider {
  override suspend fun currentToken(): String {
    return currentAccessToken
  }

  override suspend fun renewToken(previousToken: String): String {
    return newAccessToken
  }
}