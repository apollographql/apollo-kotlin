package com.apollographql.apollo.internal.incremental

import com.apollographql.apollo.network.IncrementalDeliveryProtocol

internal sealed interface IncrementalDeliveryProtocolImpl {
  val acceptHeader: String

  fun newIncrementalResultsMerger(): IncrementalResultsMerger

  object DraftInitial : IncrementalDeliveryProtocolImpl {
    override val acceptHeader: String = "multipart/mixed;deferSpec=20220824, application/graphql-response+json, application/json"

    override fun newIncrementalResultsMerger(): IncrementalResultsMerger = DraftInitialIncrementalResultsMerger()
  }

  object Draft0_1 : IncrementalDeliveryProtocolImpl {
    // TODO To be agreed upon with the router and other clients
    override val acceptHeader: String =
      "multipart/mixed;incrementalDeliverySpec=20230621, application/graphql-response+json, application/json"

    override fun newIncrementalResultsMerger(): IncrementalResultsMerger = Draft0_1IncrementalResultsMerger()
  }
}

internal val IncrementalDeliveryProtocol.impl: IncrementalDeliveryProtocolImpl
  get() = when (this) {
    IncrementalDeliveryProtocol.DraftInitial -> IncrementalDeliveryProtocolImpl.DraftInitial
    IncrementalDeliveryProtocol.Draft0_1 -> IncrementalDeliveryProtocolImpl.Draft0_1
  }
