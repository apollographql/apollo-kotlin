package com.apollographql.apollo3.internal

import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.api.ApolloRequest

@ApolloInternal
interface ApolloClientListener {
  fun requestStarted(request: ApolloRequest<*>)
  fun requestCompleted(request: ApolloRequest<*>)
}