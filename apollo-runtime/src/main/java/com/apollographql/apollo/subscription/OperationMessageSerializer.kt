package com.apollographql.apollo.subscription

import okio.BufferedSink
import okio.BufferedSource
import java.io.IOException

interface OperationMessageSerializer {
  @Throws(IOException::class)
  fun OperationClientMessage.writeTo(sink: BufferedSink)

  @Throws(IOException::class)
  fun readServerMessage(source: BufferedSource): OperationServerMessage
}