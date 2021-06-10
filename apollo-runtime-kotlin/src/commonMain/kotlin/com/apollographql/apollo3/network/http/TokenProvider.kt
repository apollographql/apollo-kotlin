package com.apollographql.apollo3.network.http

interface TokenProvider {
  suspend fun currentToken(): String
  suspend fun refreshToken(previousToken: String): String
}