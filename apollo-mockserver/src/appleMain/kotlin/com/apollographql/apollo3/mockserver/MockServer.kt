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

/**
 * @param acceptDelayMillis: an artificial delay introduced before each `accept()`
 * call. Can be used to simulate slow connections.
 */
@OptIn(ExperimentalStdlibApi::class)
actual class MockServer(
    private val acceptDelayMillis: Long,
    override val mockServerHandler: MockServerHandler = QueueMockServerHandler(),
) : MockServerInterface {

  init {
    check(isExperimentalMM()) {
      "Apollo: The legacy memory manager is no longer supported, please use the new memory manager instead. " +
          "See https://github.com/JetBrains/kotlin/blob/master/kotlin-native/NEW_MM.md for more information."
    }
  }

  actual constructor(mockServerHandler: MockServerHandler) : this(0, mockServerHandler)

  private val pthreadT: pthread_tVar
  private val port: Int
  private var socket: Socket? = null

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

    socket = Socket(socketFd, acceptDelayMillis, mockServerHandler)

    val stableRef = StableRef.create(socket!!)

    pthread_create(pthreadT.ptr, null, staticCFunction { arg ->
      initRuntimeIfNeeded()

      val ref = arg!!.asStableRef<Socket>()

      try {
        ref.get().also {
          ref.dispose()
        }.run()
      } catch (e: Throwable) {
        println("MockServer socket thread crashed: $e")
        e.printStackTrace()
      }

      null
    }, stableRef.asCPointer())
  }

  override suspend fun url(): String {
    return "http://localhost:$port/"
  }

  override fun enqueue(mockResponse: MockResponse) {
    check(socket != null) {
      "Cannot enqueue a response to a stopped MockServer"
    }
    val queueMockServerHandler = mockServerHandler as? QueueMockServerHandler
        ?: error("Apollo: cannot call MockServer.enqueue() with a custom handler")
    queueMockServerHandler.enqueue(mockResponse)
  }

  /**
   * [MockServer] can only stop in between complete request/responses pairs
   * If stop() is called while we're reading a request, this might wait forever
   * Revisit once okio has native Timeout
   */
  override suspend fun stop() {
    if (socket == null) {
      return
    }
    socket!!.stop()
    pthread_join(pthreadT.value, null)

    pthreadT.value = null
    nativeHeap.free(pthreadT.rawPtr)

    socket = null
  }

  override fun takeRequest(): MockRequest {
    check(socket != null) {
      "Cannot take a request from a stopped MockServer"
    }
    return socket!!.takeRequest()
  }
}
