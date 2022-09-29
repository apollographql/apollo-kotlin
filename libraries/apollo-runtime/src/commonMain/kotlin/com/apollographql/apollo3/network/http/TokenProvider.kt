package com.apollographql.apollo3.network.http

import com.apollographql.apollo3.annotations.ApolloDeprecatedSince

@Deprecated("BearerTokenInterceptor was provided as an example but is too simple for most use cases." +
    "Define your own interceptor or take a look at https://www.apollographql.com/docs/kotlin/advanced/interceptors-http" +
    " for more details.")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v3_2_3)
interface TokenProvider {
  suspend fun currentToken(): String
  suspend fun refreshToken(previousToken: String): String
}