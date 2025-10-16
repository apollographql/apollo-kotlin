package com.apollographql.apollo.internal.incremental

import com.apollographql.apollo.network.IncrementalDeliveryProtocol

internal sealed interface IncrementalDeliveryProtocolImpl {
  val acceptHeader: String

  fun newIncrementalResultsMerger(): IncrementalResultsMerger

  object V0_0 : IncrementalDeliveryProtocolImpl {
    override val acceptHeader: String = "multipart/mixed;deferSpec=20220824, application/graphql-response+json, application/json"

    override fun newIncrementalResultsMerger(): IncrementalResultsMerger = V0_0IncrementalResultsMerger()
  }

  object V0_1 : IncrementalDeliveryProtocolImpl {
    override val acceptHeader: String = "multipart/mixed;incrementalSpec=v0.1, application/graphql-response+json, application/json"

    override fun newIncrementalResultsMerger(): IncrementalResultsMerger = V0_1IncrementalResultsMerger()
  }
}

internal val IncrementalDeliveryProtocol.impl: IncrementalDeliveryProtocolImpl
  get() = when (this) {
    IncrementalDeliveryProtocol.V0_0 -> IncrementalDeliveryProtocolImpl.V0_0
    IncrementalDeliveryProtocol.V0_1 -> IncrementalDeliveryProtocolImpl.V0_1
  }
