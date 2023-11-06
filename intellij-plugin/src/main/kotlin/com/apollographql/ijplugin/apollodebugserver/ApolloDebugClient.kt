package com.apollographql.ijplugin.apollodebugserver

import com.android.ddmlib.IDevice
import com.android.tools.idea.adb.AdbShellCommandsUtil
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.debug.GetApolloClientsQuery
import com.apollographql.apollo3.debug.GetNormalizedCacheQuery
import com.apollographql.ijplugin.util.logw
import java.io.Closeable

private const val SOCKET_NAME_PREFIX = "apollo_debug_"
private const val BASE_PORT = 12200

class ApolloDebugClient(
    private val device: IDevice,
    val packageName: String,
) : Closeable {
  companion object {
    private var uniquePort = 0

    private fun getUniquePort(): Int {
      return BASE_PORT + uniquePort++
    }

    private fun IDevice.getApolloDebugPackageList(): Result<List<String>> {
      val commandResult = runCatching {
        AdbShellCommandsUtil.create(this).executeCommandBlocking("cat /proc/net/unix | grep $SOCKET_NAME_PREFIX | cat")
      }
      if (commandResult.isFailure) {
        val e = commandResult.exceptionOrNull()!!
        logw(e, "Could not list Apollo Debug packages")
        return Result.failure(e)
      }
      val result = commandResult.getOrThrow()
      if (result.isError) {
        val message = "Could not list Apollo Debug packages: ${result.output.joinToString()}"
        logw(message)
        return Result.failure(Exception(message))
      }
      // Results are in the form:
      // 0000000000000000: 00000002 00000000 00010000 0001 01 116651 @apollo_debug_com.example.myapplication
      return Result.success(
          result.output
              .filter { it.contains(SOCKET_NAME_PREFIX) }
              .map { it.substringAfterLast(SOCKET_NAME_PREFIX) }
              .sorted()
      )
    }

    fun IDevice.getApolloDebugClients(): Result<List<ApolloDebugClient>> {
      return getApolloDebugPackageList().map { packageNames ->
        packageNames.map { packageName ->
          ApolloDebugClient(this, packageName)
        }
      }
    }
  }

  private val port = getUniquePort()
  private var hasPortForward: Boolean = false

  private val apolloClient = ApolloClient.Builder()
      .serverUrl("http://localhost:$port")
      .build()

  private fun createPortForward() {
    device.createForward(port, "$SOCKET_NAME_PREFIX$packageName", IDevice.DeviceUnixSocketNamespace.ABSTRACT)
    hasPortForward = true
  }

  private fun removePortForward() {
    device.removeForward(port)
    hasPortForward = false
  }

  private fun ensurePortForward() {
    if (!hasPortForward) {
      createPortForward()
    }
  }

  suspend fun getApolloClients(): Result<List<GetApolloClientsQuery.ApolloClient>> = runCatching {
    ensurePortForward()
    apolloClient.query(GetApolloClientsQuery()).execute().dataOrThrow().apolloClients
  }

  suspend fun getNormalizedCache(
      apolloClientId: String,
      normalizedCacheId: String,
  ): Result<GetNormalizedCacheQuery.NormalizedCache> = runCatching {
    ensurePortForward()
    apolloClient.query(GetNormalizedCacheQuery(apolloClientId, normalizedCacheId)).execute().dataOrThrow().apolloClient?.normalizedCache
        ?: error("No normalized cache returned by server")
  }

  override fun close() {
    if (hasPortForward) {
      removePortForward()
    }
    apolloClient.close()
  }
}

val String.normalizedCacheSimpleName: String
  get() = when (this) {
    "com.apollographql.apollo3.cache.normalized.api.MemoryCache" -> "MemoryCache"
    "com.apollographql.apollo3.cache.normalized.sql.SqlNormalizedCache" -> "SqlNormalizedCache"
    else -> this
  }
