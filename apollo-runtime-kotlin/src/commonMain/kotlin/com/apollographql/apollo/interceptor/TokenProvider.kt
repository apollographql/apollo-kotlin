package com.apollographql.apollo.interceptor

interface TokenProvider {
  suspend fun currentToken(): String
  suspend fun renewToken(): String
}