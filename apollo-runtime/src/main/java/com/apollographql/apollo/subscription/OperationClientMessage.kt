package com.apollographql.apollo.subscription

import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.Subscription
import com.apollographql.apollo.api.internal.json.JsonWriter
import okio.Buffer
import java.io.IOException

sealed class OperationClientMessage {
  @Deprecated("This method is deprecated. Use an OperationMessageSerializer instead.")
  fun toJsonString(): String =
      try {
        val buffer = Buffer()
        ApolloOperationMessageSerializer.writeClientMessage(this, buffer)
        buffer.readUtf8()
      } catch (e: IOException) {
        throw RuntimeException("Failed to serialize to json", e)
      }

  @Suppress("DeprecatedCallableAddReplaceWith")
  @Throws(IOException::class)
  @Deprecated("This method is deprecated. Use an OperationMessageSerializer instead.")
  fun writeToJson(writer: JsonWriter) {
    with(ApolloOperationMessageSerializer) { writeContentsTo(writer) }
  }

  class Init(@JvmField val connectionParams: Map<String, Any?>) : OperationClientMessage() {
    companion object {
      internal const val TYPE = "connection_init"
    }
  }

  class Start(
      @JvmField
      val subscriptionId: String,
      @JvmField
      val subscription: Subscription<*, *>,
      @JvmField
      val customScalarAdapters: CustomScalarAdapters,
      @JvmField
      val autoPersistSubscription: Boolean,
      @JvmField
      val sendSubscriptionDocument: Boolean
  ) : OperationClientMessage() {
    companion object {
      internal const val TYPE = "start"
    }
  }

  class Stop(@JvmField val subscriptionId: String) : OperationClientMessage() {
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