package com.apollographql.apollo3.interceptor

interface TokenProvider {
  suspend fun currentToken(): String
  suspend fun refreshToken(previousToken: String): String
}