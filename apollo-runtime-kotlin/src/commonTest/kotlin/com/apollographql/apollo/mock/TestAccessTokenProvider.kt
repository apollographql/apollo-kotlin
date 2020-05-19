package com.apollographql.apollo.mock

import com.apollographql.apollo.interceptor.AccessTokenProvider

class TestAccessTokenProvider(val currentAccessToken: String,
                              val newAccessToken: String) : AccessTokenProvider {
  override suspend fun currentAccessToken(): String {
    return currentAccessToken
  }

  override suspend fun newAccessToken(previousToken: String): String {
    return newAccessToken
  }
}