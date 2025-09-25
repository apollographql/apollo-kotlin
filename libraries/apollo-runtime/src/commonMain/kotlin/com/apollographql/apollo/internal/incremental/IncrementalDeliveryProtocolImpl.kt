package com.apollographql.apollo.internal.incremental

import com.apollographql.apollo.network.http.HttpNetworkTransport

internal sealed interface IncrementalDeliveryProtocolImpl {
  val acceptHeader: String

  fun newIncrementalResultsMerger(): IncrementalResultsMerger

  /**
   * Format specified in this historical commit:
   * https://github.com/graphql/graphql-spec/tree/48cf7263a71a683fab03d45d309fd42d8d9a6659/spec
   *
   * Only `@defer` is supported with this format.
   *
   * This is the default.
   */
  object Defer20220824 : IncrementalDeliveryProtocolImpl {
    override val acceptHeader: String = "multipart/mixed;deferSpec=20220824, application/graphql-response+json, application/json"

    override fun newIncrementalResultsMerger(): IncrementalResultsMerger = GraphQL17Alpha2IncrementalResultsMerger()
  }

  /**
   * Newer format as implemented by graphql.js version `17.0.0-alpha.9`.
   *
   * Both `@defer` and `@stream` are supported with this format.
   */
  object GraphQL17Alpha9 : IncrementalDeliveryProtocolImpl {
    // TODO To be agreed upon with the router and other clients
    override val acceptHeader: String =
      "multipart/mixed;incrementalDeliverySpec=20230621, application/graphql-response+json, application/json"

    override fun newIncrementalResultsMerger(): IncrementalResultsMerger = GraphQL17Alpha9IncrementalResultsMerger()
  }
}

internal val HttpNetworkTransport.IncrementalDeliveryProtocol.impl: IncrementalDeliveryProtocolImpl
  get() = when (this) {
    HttpNetworkTransport.IncrementalDeliveryProtocol.GraphQL17Alpha2 -> IncrementalDeliveryProtocolImpl.Defer20220824
    HttpNetworkTransport.IncrementalDeliveryProtocol.GraphQL17Alpha9 -> IncrementalDeliveryProtocolImpl.GraphQL17Alpha9
  }
