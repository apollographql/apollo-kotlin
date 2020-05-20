package com.apollographql.apollo.interceptor

interface AccessTokenProvider {
  suspend fun currentToken(): String
  suspend fun renewToken(): String
}