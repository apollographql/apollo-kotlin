package com.apollographql.apollo.internal

import com.apollographql.apollo.annotations.ApolloInternal
import com.apollographql.apollo.api.ApolloRequest

@ApolloInternal
interface ApolloClientListener {
  fun requestStarted(request: ApolloRequest<*>)
  fun requestCompleted(request: ApolloRequest<*>)
}