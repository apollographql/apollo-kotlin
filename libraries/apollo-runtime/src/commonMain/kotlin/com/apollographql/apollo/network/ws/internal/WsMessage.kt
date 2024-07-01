package com.apollographql.apollo.network.ws.internal

import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.Operation

internal sealed interface Message
internal sealed interface Command : Message
internal class StartOperation<D : Operation.Data>(val request: ApolloRequest<D>) : Command
internal class StopOperation<D : Operation.Data>(val request: ApolloRequest<D>) : Command
internal object RestartConnection: Command
internal object Dispose: Command

internal sealed interface Event : Message {
  /**
   * the id of the operation
   * Might be null for general errors or network errors that are broadcast to all listeners
   */
  val id: String?
}

internal class OperationResponse(override val id: String?, val payload: Map<String, Any?>) : Event
internal class OperationError(override val id: String?, val payload: Map<String, Any?>?) : Event
internal class OperationComplete(override val id: String?) : Event
internal class ConnectionReEstablished : Event {
  override val id: String? = null
}
internal class GeneralError(val payload: Map<String, Any?>?) : Event {
  override val id: String? = null
}

internal class NetworkError(val cause: Throwable) : Event {
  override val id: String? = null
}
