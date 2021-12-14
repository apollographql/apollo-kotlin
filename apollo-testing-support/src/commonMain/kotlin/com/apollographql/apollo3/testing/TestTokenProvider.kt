package com.apollographql.apollo3.testing

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.network.http.TokenProvider

@ApolloExperimental
class TestTokenProvider(currentAccessToken: String,
                        val newAccessToken: String) : TokenProvider {
  var accessToken = currentAccessToken
  override suspend fun currentToken(): String {
    return accessToken
  }

  override suspend fun refreshToken(previousToken: String): String {
    accessToken = newAccessToken
    return accessToken
  }
}