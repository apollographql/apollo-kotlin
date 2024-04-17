package com.apollographql.ijplugin.normalizedcache

import com.android.adblib.syncRecv
import com.android.ddmlib.AndroidDebugBridge
import com.android.ddmlib.IDevice
import com.android.tools.idea.adb.AdbShellCommandsUtil
import com.android.tools.idea.adblib.AdbLibApplicationService
import com.android.tools.idea.adblib.ddmlibcompatibility.toDeviceSelector
import com.apollographql.ijplugin.util.execute
import com.apollographql.ijplugin.util.executeCatching
import com.apollographql.ijplugin.util.logd
import com.apollographql.ijplugin.util.logw
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

val isAndroidPluginPresent = try {
  Class.forName("com.android.ddmlib.AndroidDebugBridge")
  true
} catch (e: ClassNotFoundException) {
  false
}

fun getConnectedDevices(): List<IDevice> {
  return AndroidDebugBridge.createBridge(1, TimeUnit.SECONDS).devices.sortedBy { it.name }
}

fun IDevice.getDebuggablePackageList(): Result<List<String>> {
  val commandResult = AdbShellCommandsUtil.create(this).executeCatching(
      // List all packages, and try run-as on them - if it succeeds, the package is debuggable
      "for p in \$(pm list packages -3 | cut -d : -f 2); do (run-as \$p true >/dev/null 2>&1 && echo \$p); done; true"
  )
  if (commandResult.isFailure) {
    val e = commandResult.exceptionOrNull()!!
    logw(e, "Could not list debuggable packages")
    return Result.failure(e)
  }
  val result = commandResult.getOrThrow()
  if (result.isError) {
    val message = "Could not list debuggable packages: ${result.output.joinToString()}"
    logw(message)
    return Result.failure(Exception(message))
  }
  return Result.success(result.output.filterNot { it.isEmpty() }.sorted())
}

fun IDevice.getDatabaseList(packageName: String, databasesDir: String): Result<List<String>> {
  val commandResult = AdbShellCommandsUtil.create(this).executeCatching("run-as $packageName ls -1 $databasesDir")
  if (commandResult.isFailure) {
    val e = commandResult.exceptionOrNull()!!
    logw(e, "Could not list databases")
    return Result.failure(e)
  }
  val result = commandResult.getOrThrow()
  if (result.isError) {
    if (result.output.any { it.contains("No such file or directory") }) {
      return Result.success(emptyList())
    }
    val message = "Could not list databases: ${result.output.joinToString()}"
    logw(message)
    return Result.failure(Exception(message))
  }
  return Result.success(result.output.filter { it.isDatabaseFileName() }.sorted())
}

suspend fun pullFile(device: IDevice, appPackageName: String, remoteDirName: String, remoteFileName: String): Result<File> {
  val remoteFilePath = "$remoteDirName/$remoteFileName"
  val localFile = File.createTempFile(remoteFileName.substringBeforeLast(".") + "-tmp", ".db")
  logd("Pulling $remoteFilePath to ${localFile.absolutePath}")
  val intermediateRemoteFilePath = "/data/local/tmp/${localFile.name}"
  val shellCommandsUtil = AdbShellCommandsUtil.create(device)
  return runCatching {
    var commandResult = shellCommandsUtil.execute("touch $intermediateRemoteFilePath")
    if (commandResult.isError) {
      throw Exception("'touch' command failed")
    }
    commandResult = shellCommandsUtil.execute("run-as $appPackageName sh -c 'cp $remoteFilePath $intermediateRemoteFilePath'")
    if (commandResult.isError) {
      throw Exception("'copy' command failed")
    }
    try {
      val adbLibSession = AdbLibApplicationService.instance.session
      val fileChannel = adbLibSession.channelFactory.createFile(Paths.get(localFile.absolutePath))
      fileChannel.use {
        adbLibSession.deviceServices.syncRecv(device.toDeviceSelector(), intermediateRemoteFilePath, fileChannel)
      }
    } finally {
      commandResult = shellCommandsUtil.execute("rm $intermediateRemoteFilePath")
      if (commandResult.isError) {
        logw("'rm' command failed")
      }
    }
    localFile
  }
}

// See https://www.sqlite.org/tempfiles.html
fun String.isDatabaseFileName() = isNotEmpty() && !endsWith("-journal") && !endsWith("-wal") && !endsWith("-shm")
