package com.apollographql.apollo3.integration

import com.apollographql.apollo3.testing.runBlocking
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Delay
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.CoreFoundation.CFRunLoopGetCurrent
import platform.CoreFoundation.CFRunLoopGetMain
import platform.CoreFoundation.CFRunLoopRun
import platform.CoreFoundation.CFRunLoopStop
import platform.Foundation.NSRunLoop
import platform.Foundation.NSRunLoopCommonModes
import platform.Foundation.run
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.dispatch_after
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_main
import platform.darwin.dispatch_time
import platform.posix.CLOCK_REALTIME
import platform.posix.clock_gettime
import platform.posix.sleep
import platform.posix.timespec
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test


class MockServerTest {
  @Test
  fun test() {
    val server = MockServer()

    server.enqueue(MockResponse(
        200,
        "{)"
    ))

    runBlocking {
      delay(30000)
    }
    println("killing server")
    server.stop()

    println("server killed")
    runBlocking {
      while(true) {
        delay(5000)
      }
    }
  }
}