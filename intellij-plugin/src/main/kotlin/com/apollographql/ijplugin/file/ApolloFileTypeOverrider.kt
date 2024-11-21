package com.apollographql.ijplugin.file

import com.apollographql.ijplugin.settings.appSettingsState
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.impl.FileTypeOverrider
import com.intellij.openapi.vfs.VirtualFile

@Suppress("UnstableApiUsage")
class ApolloFileTypeOverrider : FileTypeOverrider {
  override fun getOverriddenFileType(file: VirtualFile): FileType? {
    return if (file.extension in ApolloGraphQLFileType.SUPPORTED_EXTENSIONS && appSettingsState.lspModeEnabled) {
      ApolloGraphQLFileType.INSTANCE
    } else {
      null
    }
  }
}
