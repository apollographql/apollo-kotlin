package com.apollographql.apollo.interceptor

interface TokenProvider {
  suspend fun currentToken(): String
  suspend fun refreshToken(previousToken: String): String
}