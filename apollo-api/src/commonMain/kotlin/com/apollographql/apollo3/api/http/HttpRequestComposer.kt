package com.apollographql.apollo3.api.http

import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.Operation

/**
 * [HttpRequestComposer] transforms a GraphQL request in a [HttpRequest]. Typically, this involves building a
 * Json with "query" and "variables" fields but implementations can decide to customize this behaviour.
 * For an example:
 * - to skip sending the "query" if the server has support for query whitelisting
 * - to add extensions to the Post payload
 *
 * See [DefaultHttpRequestComposer]
 */
interface HttpRequestComposer {
  fun <D : Operation.Data> compose(apolloRequest: ApolloRequest<D>): HttpRequest
}

