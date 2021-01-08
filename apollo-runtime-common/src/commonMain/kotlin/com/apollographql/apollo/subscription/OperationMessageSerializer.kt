package com.apollographql.apollo.subscription

import okio.BufferedSink
import okio.BufferedSource

/**
 * An operation message serializer is responsible for converting to and from the transport format used for web socket subscriptions.
 *
 * @see ApolloOperationMessageSerializer
 * @see AppSyncOperationMessageSerializer
 */
interface OperationMessageSerializer {
  /**
   * Writes the given [OperationClientMessage] to the given [sink][BufferedSink].
   *
   * Will propagate any exceptions thrown by the sink.
   *
   * @param message The message to write.
   * @param sink The sink to write to.
   */
  fun writeClientMessage(message: OperationClientMessage, sink: BufferedSink)

  /**
   * Reads an [OperationServerMessage] from the given [source][BufferedSource].
   *
   * Will propagate any [IOException]'s thrown by the source, but unknown or invalid messages are handled by returning
   * [OperationServerMessage.Unsupported].
   *
   * @param source The source to read from
   * @return The read message.
   */
  fun readServerMessage(source: BufferedSource): OperationServerMessage
}