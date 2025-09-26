package com.apollographql.apollo.internal.incremental

import com.apollographql.apollo.network.http.HttpNetworkTransport

internal sealed interface IncrementalDeliveryProtocolImpl {
  val acceptHeader: String

  fun newIncrementalResultsMerger(): IncrementalResultsMerger

  object GraphQL17Alpha2 : IncrementalDeliveryProtocolImpl {
    override val acceptHeader: String = "multipart/mixed;deferSpec=20220824, application/graphql-response+json, application/json"

    override fun newIncrementalResultsMerger(): IncrementalResultsMerger = GraphQL17Alpha2IncrementalResultsMerger()
  }

  object GraphQL17Alpha9 : IncrementalDeliveryProtocolImpl {
    // TODO To be agreed upon with the router and other clients
    override val acceptHeader: String =
      "multipart/mixed;incrementalDeliverySpec=20230621, application/graphql-response+json, application/json"

    override fun newIncrementalResultsMerger(): IncrementalResultsMerger = GraphQL17Alpha9IncrementalResultsMerger()
  }
}

internal val HttpNetworkTransport.IncrementalDeliveryProtocol.impl: IncrementalDeliveryProtocolImpl
  get() = when (this) {
    HttpNetworkTransport.IncrementalDeliveryProtocol.GraphQL17Alpha2 -> IncrementalDeliveryProtocolImpl.GraphQL17Alpha2
    HttpNetworkTransport.IncrementalDeliveryProtocol.GraphQL17Alpha9 -> IncrementalDeliveryProtocolImpl.GraphQL17Alpha9
  }
