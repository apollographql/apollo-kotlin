package com.apollographql.apollo.subscription

import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.Subscription
import com.apollographql.apollo.api.internal.json.JsonWriter
import okio.Buffer

sealed class OperationClientMessage {
  class Init(val connectionParams: Map<String, Any?>) : OperationClientMessage() {
    companion object {
      internal const val TYPE = "connection_init"
    }
  }

  class Start(
      val subscriptionId: String,
      val subscription: Subscription<*>,
      val customScalarAdapters: CustomScalarAdapters,
      val autoPersistSubscription: Boolean,
      /**
       * whether or not to send the document. Only valid if [autoPersistSubscription] is true
       */
      val sendSubscriptionDocument: Boolean
  ) : OperationClientMessage() {
    companion object {
      internal const val TYPE = "start"
    }
  }

  class Stop(val subscriptionId: String) : OperationClientMessage() {
    companion object {
      internal const val TYPE = "stop"
    }
  }

  class Terminate : OperationClientMessage() {
    companion object {
      internal const val TYPE = "connection_terminate"
    }
  }
}