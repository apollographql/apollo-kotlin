package com.apollographql.ijplugin.normalizedcache

import com.android.ddmlib.IDevice
import com.apollographql.ijplugin.apollodebugserver.ApolloDebugClient
import java.io.File

sealed interface NormalizedCacheSource {
  data class LocalFile(val file: File) : NormalizedCacheSource

  data class DeviceFile(
      val device: IDevice,
      val packageName: String,
      val remoteDirName: String,
      val remoteFileName: String,
  ) : NormalizedCacheSource

  data class ApolloDebugServer(
      val apolloDebugClient: ApolloDebugClient,
      val apolloClientId: String,
      val normalizedCacheId: String,
  ) : NormalizedCacheSource
}
