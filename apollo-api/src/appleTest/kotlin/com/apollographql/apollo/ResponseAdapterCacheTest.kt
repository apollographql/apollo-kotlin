package com.apollographql.apollo

import com.apollographql.apollo.api.ResponseAdapterCache
import com.apollographql.apollo.api.internal.IntResponseAdapter
import com.apollographql.apollo.api.internal.json.BufferedSourceJsonReader
import okio.Buffer
import kotlin.native.concurrent.AtomicInt
import kotlin.native.concurrent.ThreadLocal
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.native.concurrent.freeze
import kotlin.test.Test
import kotlin.test.assertEquals

@ThreadLocal
var localThreadId = 0

@SharedImmutable
private val threadIdCounter = AtomicInt(1)

private fun currentThreadId(): Int {
  if (localThreadId == 0) {
    localThreadId = threadIdCounter.addAndGet(1)
  }
  return localThreadId
}

class ResponseAdapterCacheTest {
  @Test
  fun `accessing ResponseAdapterCache from 2 threads`() {
    val cache = ResponseAdapterCache(emptyMap())

    println("Main thread is: ${currentThreadId()}")

    val fromMain = readInt(cache)
    assertEquals(42, fromMain)

    val fromWork = Worker.start(name = "testWorker").execute(TransferMode.SAFE, { cache.freeze() }) {
      println("Worker thre ad is: ${currentThreadId()}")
      readInt(it)
    }.result

    assertEquals(42, fromWork)
  }
}

private fun readInt(cache: ResponseAdapterCache): Int {
  val adapter = cache.getOperationAdapter("TestQuery") { IntResponseAdapter }
  val buffer = Buffer()
  buffer.writeUtf8("42")
  val jsonReader = BufferedSourceJsonReader(buffer)
  return adapter.fromResponse(jsonReader)
}