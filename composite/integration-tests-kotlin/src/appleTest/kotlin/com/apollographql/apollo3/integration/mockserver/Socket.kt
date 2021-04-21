package com.apollographql.apollo3.integration.mockserver

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.nativeHeap
import okio.buffer
import platform.posix.POLLIN
import platform.posix.accept
import platform.posix.close
import platform.posix.errno
import platform.posix.pipe
import platform.posix.poll
import platform.posix.pollfd
import platform.posix.write
import kotlin.experimental.and
import kotlin.native.concurrent.AtomicInt
import kotlin.native.concurrent.AtomicReference
import kotlin.native.concurrent.freeze

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

  private inline fun debug(message: String) {
    if (false) {
      println(message)
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

        debug("Read request")

        val request = readRequest(source)

        debug("Got request: ${request.method} ${request.path}")

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

        debug("Write response: ${mockResponse.statusCode}")

        writeResponse(sink, mockResponse, request.version)

        debug("Response Written")
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