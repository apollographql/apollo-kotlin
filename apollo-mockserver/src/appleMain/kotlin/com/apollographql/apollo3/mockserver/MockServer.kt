package com.apollographql.apollo3.mockserver

import kotlinx.cinterop.StableRef
import kotlinx.cinterop.UIntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.value
import platform.posix.AF_INET
import platform.posix.INADDR_ANY
import platform.posix.SOCK_STREAM
import platform.posix.bind
import platform.posix.errno
import platform.posix.getsockname
import platform.posix.listen
import platform.posix.pthread_create
import platform.posix.pthread_join
import platform.posix.pthread_tVar
import platform.posix.sockaddr_in
import platform.posix.socket
import kotlin.native.concurrent.freeze

@OptIn(ExperimentalUnsignedTypes::class)
actual class MockServer {
  private val pthreadT: pthread_tVar
  private val port: Int
  private val socket: Socket

  init {
    val socketFd = socket(AF_INET, SOCK_STREAM, 0)

    check(socketFd != -1) {
      "Cannot open socket (errno = $errno)"
    }

    port = memScoped {
      val sockaddrIn = alloc<sockaddr_in>().apply {
        sin_family = AF_INET.convert()
        sin_port = 0.convert() //htons(port.convert())
        sin_addr.s_addr = INADDR_ANY // AutoFill local address
      }

      check(bind(socketFd, sockaddrIn.ptr.reinterpret(), sizeOf<sockaddr_in>().convert()) == 0) {
        "Cannot bind socket (errno = $errno)"
      }
      val addrLen = alloc<UIntVar>()
      addrLen.value = sizeOf<sockaddr_in>().convert()
      getsockname(socketFd, sockaddrIn.ptr.reinterpret(), addrLen.ptr)

      val networkPort = sockaddrIn.sin_port.toInt()
      // Convert to MacOS endianess, this is most likely wrong on other systems but I can't find a ntohs
      networkPort.and(0xff).shl(8).or(networkPort.and(0xff00).shr(8))
    }

    listen(socketFd, 1)

    pthreadT = nativeHeap.alloc()

    socket = Socket(socketFd)

    val stableRef = StableRef.create(socket.freeze())

    pthread_create(pthreadT.ptr, null, staticCFunction { arg ->
      initRuntimeIfNeeded()

      val ref = arg!!.asStableRef<Socket>()

      ref.get().also {
        ref.dispose()
      }.run()

      null
    }, stableRef.asCPointer())
  }

  actual fun url(): String {
    return "http://localhost:$port"
  }

  actual fun enqueue(mockResponse: MockResponse) {
    socket.enqueue(mockResponse)
  }

  /**
   * [MockServer] can only stop in between complete request/responses pairs
   * If stop() is called while we're reading a request, this might wait forever
   * Revisit once okio has native Timeout
   */
  actual fun stop() {
    socket.stop()
    pthread_join(pthreadT.value, null)

    nativeHeap.free(pthreadT.rawPtr)
  }

  actual fun takeRequest(): MockRecordedRequest {
    return socket.takeRequest()
  }
}