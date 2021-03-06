package com.apollographql.apollo3.integration

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.UIntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.convert
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.value
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okio.buffer
import platform.posix.AF_INET
import platform.posix.INADDR_ANY
import platform.posix.POLLIN
import platform.posix.SOCK_STREAM
import platform.posix.accept
import platform.posix.bind
import platform.posix.close
import platform.posix.errno
import platform.posix.getsockname
import platform.posix.listen
import platform.posix.pipe
import platform.posix.poll
import platform.posix.pollfd
import platform.posix.pthread_create
import platform.posix.pthread_join
import platform.posix.pthread_tVar
import platform.posix.sleep
import platform.posix.sockaddr_in
import platform.posix.socket
import platform.posix.write
import kotlin.experimental.and
import kotlin.native.concurrent.AtomicInt
import kotlin.native.concurrent.AtomicReference
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

    println("Bound to port $port")

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

  actual fun takeRequest(): RecordedRequest {
    return socket.takeRequest()
  }
}

class Socket(private val socketFd: Int) {
  private val pipeFd = nativeHeap.allocArray<IntVar>(2)
  private val running = AtomicInt(1)
  private val lock = SynchronizedObject()
  private val responseQueue = AtomicReference<List<MockResponse>>(emptyList())
  private val requestQueue = AtomicReference<List<RecordedRequest>>(emptyList())

  init {
    check(pipe(pipeFd) == 0) {
      "Cannot create pipe (errno=$errno)"
    }
  }

  fun run() {
    while (running.value != 0) {
      memScoped {
        val fdSet = allocArray<pollfd>(2)

        fdSet[0].fd = socketFd
        fdSet[0].events = POLLIN.convert()
        fdSet[1].fd = pipeFd[0]
        fdSet[1].events = POLLIN.convert()

        // the timeout is certainly not required but since we're not locking running.value
        // I guess there's a chance for a small race
        poll(fdSet, 2.convert(), 1000.convert())

        if (fdSet[0].revents.and(POLLIN.convert()).toInt() == 0) {
          return@memScoped
        }

        // wait for a new incoming connection
        val connectionFd = accept(socketFd, null, null)

        check(connectionFd >= 0) {
          "Cannot accept socket (errno = $errno)"
        }

        handleConnection(connectionFd)
      }
    }
    close(socketFd)
  }

  private fun handleConnection(connectionFd: Int) {
    val source = FileDescriptorSource(connectionFd).buffer()
    val sink = FileDescriptorSink(connectionFd).buffer()

    while (running.value != 0) {
      memScoped {
        val fdSet = allocArray<pollfd>(2)

        fdSet[0].fd = connectionFd
        fdSet[0].events = POLLIN.convert()
        fdSet[1].fd = pipeFd[0]
        fdSet[1].events = POLLIN.convert()

        // the timeout is certainly not required but since we're not locking running.value
        // I guess there's a chance for a small race
        poll(fdSet, 2.convert(), 1000.convert())

        if (fdSet[0].revents.and(POLLIN.convert()).toInt() == 0) {
          return@memScoped
        }

        val request = readRequest(source)

        val mockResponse = synchronized(lock) {
          val requests = requestQueue.value
          val newRequests = requests + request
          requestQueue.value = newRequests.freeze()

          val responses = responseQueue.value
          check(responses.isNotEmpty()) {
            "No response enqueued"
          }
          val response = responses.last()
          val newResponses = responses.dropLast(1)
          responseQueue.value = newResponses.freeze()
          response
        }

        writeResponse(sink, mockResponse, request.version)
      }
    }
  }

  fun stop() {
    running.value = 0

    memScoped {
      val buf = allocArray<ByteVar>(1)
      // Write a placeholder byte to unblock the reader if needed
      write(pipeFd[1], buf, 1)
    }
    nativeHeap.free(pipeFd.rawValue)
  }

  fun enqueue(mockResponse: MockResponse) {
    synchronized(lock) {
      val responses = responseQueue.value
      val newResponses = responses + mockResponse
      responseQueue.value = newResponses.freeze()
    }
  }

  fun takeRequest(): RecordedRequest {
    return synchronized(lock) {
      val requests = requestQueue.value
      check(requests.isNotEmpty())

      val request = requests.last()
      requestQueue.value = requests.dropLast(1)
      request
    }
  }
}