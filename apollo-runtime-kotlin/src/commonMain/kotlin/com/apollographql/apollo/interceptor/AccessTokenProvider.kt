package com.apollographql.apollo.interceptor

interface AccessTokenProvider {
  suspend fun currentAccessToken(): String
  suspend fun newAccessToken(previousToken: String): String
}