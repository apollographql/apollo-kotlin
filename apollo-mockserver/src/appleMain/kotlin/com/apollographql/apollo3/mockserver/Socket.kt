package com.apollographql.apollo3.mockserver

import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.synchronized
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import okio.IOException
import okio.buffer
import platform.Foundation.NSMutableArray
import platform.posix.POLLIN
import platform.posix.SOL_SOCKET
import platform.posix.SO_NOSIGPIPE
import platform.posix.accept
import platform.posix.close
import platform.posix.errno
import platform.posix.pipe
import platform.posix.poll
import platform.posix.pollfd
import platform.posix.setsockopt
import platform.posix.usleep
import platform.posix.write
import kotlin.experimental.and
import kotlin.native.concurrent.AtomicInt
import kotlin.native.concurrent.freeze

class Socket(private val socketFd: Int, private val acceptDelayMillis: Long) {
  private val pipeFd = nativeHeap.allocArray<IntVar>(2)
  private val running = AtomicInt(1)
  private val lock = reentrantLock()
  private val queuedResponses = NSMutableArray()
  private val recordedRequests = NSMutableArray()

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

        if (acceptDelayMillis > 0) {
          usleep((acceptDelayMillis * 1000).convert())
        }

        // wait for a new incoming connection
        val connectionFd = accept(socketFd, null, null)

        check(connectionFd >= 0) {
          "Cannot accept socket (errno = $errno)"
        }

        val one = alloc<IntVar>()
        one.value = 1
        setsockopt(connectionFd, SOL_SOCKET, SO_NOSIGPIPE, one.ptr, 4);

        handleConnection(connectionFd)
        close(connectionFd)
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

        debug("'$connectionFd': Read request")

        val request = try {
          readRequest(source)
        } catch (e: IOException) {
          debug("'$connectionFd': readRequest error")
          return
        }
        if (request == null) {
          debug("'$connectionFd': Connection closed")
          return
        }

        debug("Got request: ${request.method} ${request.path}")

        val mockResponse = synchronized(lock) {
          recordedRequests.addObject(request.freeze())

          check(queuedResponses.count.toInt() > 0) {
            "no queued responses"
          }
          queuedResponses.objectAtIndex(0).also {
            queuedResponses.removeObjectAtIndex(0)
          } as MockResponse
        }

        debug("Write response: ${mockResponse.statusCode}")

        if (mockResponse.delayMillis > 0) {
          usleep((mockResponse.delayMillis * 1000).convert())
        }

        try {
          writeResponse(sink, mockResponse, request.version)
        } catch (e: IOException) {
          debug("'$connectionFd': writeResponse error")
          return
        }

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
      queuedResponses.addObject(mockResponse.freeze())
    }
  }

  fun takeRequest(): MockRecordedRequest {
    return synchronized(lock) {
      check(recordedRequests.count.toInt() > 0) {
        "no recorded request"
      }
      recordedRequests.objectAtIndex(0).also {
        recordedRequests.removeObjectAtIndex(0)
      } as MockRecordedRequest
    }
  }
}