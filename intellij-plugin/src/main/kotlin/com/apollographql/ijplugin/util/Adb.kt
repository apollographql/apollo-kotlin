package com.apollographql.ijplugin.util

import com.android.tools.idea.adb.AdbShellCommandResult
import com.android.tools.idea.adb.AdbShellCommandsUtil

fun AdbShellCommandsUtil.execute(command: String): AdbShellCommandResult {
  logd("Executing adb shell command: '$command'")
  val result = executeCommandBlocking(command)
  logd("adb shell command result: isError=${result.isError}, isEmpty=${result.isEmpty}, output=${result.output.joinToString("\n")}")
  return result
}

fun AdbShellCommandsUtil.executeCatching(command: String): Result<AdbShellCommandResult> = runCatching {
  execute(command)
}