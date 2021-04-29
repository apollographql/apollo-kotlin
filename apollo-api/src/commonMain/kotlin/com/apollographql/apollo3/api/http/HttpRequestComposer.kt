package com.apollographql.apollo3.api.http

import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.Operation

interface HttpRequestComposer {
  fun <D : Operation.Data> compose(apolloRequest: ApolloRequest<D>): HttpRequest
}

