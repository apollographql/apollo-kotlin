package com.apollographql.apollo3.network.ws.incubating

import com.apollographql.apollo3.api.http.HttpHeader
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.mpp.currentTimeMillis
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class WebSocketHolder(
    private val webSocketEngine: WebSocketEngine,
    private val serverUrl: String,
    private val httpHeaders: List<HttpHeader>,
    private val wsProtocol: WsProtocol,
    private val connectionAcknowledgeTimeoutMillis: Long,
    private val pingIntervalMillis: Long,
    private val idleTimeoutMillis: Long,
)  {
  private val dispatcher = Dispatchers.Default
  private val scope = CoroutineScope(dispatcher)
  private var idleJob: Job? = null
  private var subscribableWebSocket: SubscribableWebSocket? = null
  private var lock = reentrantLock()

  init {
    // Make sure we do not busy loop 
    check(idleTimeoutMillis > 0) {
      "Apollo: 'idleTimeoutMillis' must be > 0"
    }
    triggerCleanup(timeoutMillis = idleTimeoutMillis)
  }

  private fun triggerCleanup(timeoutMillis: Long): Unit = lock.withLock {
    idleJob?.cancel()
    idleJob = scope.launch {
      delay(timeoutMillis)
      val next = lock.withLock {
        cleanupLocked()
      }
      triggerCleanup(next)
    }
  }

  fun acquire(): SubscribableWebSocket = lock.withLock {
    cleanupLocked()
    if (subscribableWebSocket == null) {
      subscribableWebSocket = SubscribableWebSocket(
          webSocketEngine = webSocketEngine,
          serverUrl = serverUrl,
          httpHeaders = httpHeaders,
          dispatcher = dispatcher,
          wsProtocol = wsProtocol,
          pingIntervalMillis = pingIntervalMillis,
          connectionAcknowledgeTimeoutMillis = connectionAcknowledgeTimeoutMillis,
      )
    }
    // Mark active before startOperation to avoid a small race where cleanup() would be called after acquire() returns
    // but before startOperation() runs
    subscribableWebSocket!!.markActive()
    return subscribableWebSocket!!
  }

  private fun cleanupLocked(): Long {
    if (subscribableWebSocket != null) {
      if (subscribableWebSocket!!.shutdown) {
        subscribableWebSocket = null
        return idleTimeoutMillis
      }

      val lastActiveMillis = subscribableWebSocket!!.lastActiveMillis
      if (lastActiveMillis != 0L) {
        val elapsed = currentTimeMillis() - lastActiveMillis
        if (elapsed > idleTimeoutMillis) {
          subscribableWebSocket!!.shutdown(null, CLOSE_GOING_AWAY, "Idle")
          subscribableWebSocket = null
        } else {
          return idleTimeoutMillis - elapsed
        }
      }

    }
    return idleTimeoutMillis
  }

  fun close() = lock.withLock {
    webSocketEngine.close()
    if (subscribableWebSocket != null) {
      subscribableWebSocket!!.shutdown(null, CLOSE_GOING_AWAY, "Canceled")
      subscribableWebSocket = null
    }
  }

  fun closeCurrentConnection(reason: ApolloException?) = lock.withLock {
    if (subscribableWebSocket != null) {
      subscribableWebSocket!!.shutdown(reason, CLOSE_GOING_AWAY, "Client requested closing the connection")
      subscribableWebSocket = null
    }
  }
}
