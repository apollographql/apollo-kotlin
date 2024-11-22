package com.apollographql.ijplugin.lsp

fun isLspAvailable(): Boolean {
  return runCatching { Class.forName("com.intellij.platform.lsp.api.LspServerManager") }.isSuccess
}
