package com.apollographql.ijplugin.normalizedcache

import com.android.ddmlib.AndroidDebugBridge
import com.android.ddmlib.IDevice
import com.android.ddmlib.SyncService
import com.android.tools.idea.adb.AdbShellCommandsUtil
import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.util.logd
import com.apollographql.ijplugin.util.logw
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import java.io.File
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
  val commandResult = runCatching {
    AdbShellCommandsUtil.create(this).executeCommandBlocking(
        // List all packages, and try run-as on them - if it succeeds, the package is debuggable
        "for p in \$(pm list packages -3 | cut -d : -f 2); do (run-as \$p true >/dev/null 2>&1 && echo \$p); done; true"
    )
  }
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
  val commandResult = runCatching {
    AdbShellCommandsUtil.create(this).executeCommandBlocking("run-as $packageName ls -1 $databasesDir")
  }
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

fun pullFileAsync(
    project: Project,
    device: IDevice,
    packageName: String,
    remoteDirName: String,
    remoteFileName: String,
    onFilePullSuccess: (File) -> Unit,
    onFilePullError: (Throwable) -> Unit,
) {
  object : Task.Backgroundable(
      project,
      ApolloBundle.message("normalizedCacheViewer.pullFromDevice.pull.ongoing"),
      false,
  ) {
    override fun run(indicator: ProgressIndicator) {
      val pullResult = pullFile(device = device, appPackageName = packageName, remoteDirName = remoteDirName, remoteFileName = remoteFileName)
      invokeLater(ModalityState.any()) {
        pullResult.onSuccess {
          onFilePullSuccess(it)
        }.onFailure {
          logw(it, "Pull failed")
          onFilePullError(it)
        }
      }
    }
  }.queue()

}

private fun pullFile(device: IDevice, appPackageName: String, remoteDirName: String, remoteFileName: String): Result<File> {
  val remoteFilePath = "$remoteDirName/$remoteFileName"
  val localFile = File.createTempFile(remoteFileName.substringBeforeLast(".") + "-tmp", ".db")
  logd("Pulling $remoteFilePath to ${localFile.absolutePath}")
  val intermediateRemoteFilePath = "/data/local/tmp/${localFile.name}"
  val shellCommandsUtil = AdbShellCommandsUtil.create(device)
  return runCatching {
    var commandResult = shellCommandsUtil.executeCommandBlocking("touch $intermediateRemoteFilePath")
    if (commandResult.isError) {
      throw Exception("'touch' command failed")
    }
    commandResult = shellCommandsUtil.executeCommandBlocking("run-as $appPackageName sh -c 'cp $remoteFilePath $intermediateRemoteFilePath'")
    if (commandResult.isError) {
      throw Exception("'copy' command failed")
    }
    try {
      device.syncService.pullFile(intermediateRemoteFilePath, localFile.absolutePath, NullSyncProgressMonitor)
    } finally {
      commandResult = shellCommandsUtil.executeCommandBlocking("rm $intermediateRemoteFilePath")
      if (commandResult.isError) {
        logw("'rm' command failed")
      }
    }
    localFile
  }
}

private object NullSyncProgressMonitor : SyncService.ISyncProgressMonitor {
  override fun isCanceled() = false
  override fun start(totalWork: Int) {}
  override fun startSubTask(name: String) {}
  override fun advance(work: Int) {}
  override fun stop() {}
}

// See https://www.sqlite.org/tempfiles.html
fun String.isDatabaseFileName() = isNotEmpty() && !endsWith("-journal") && !endsWith("-wal") && !endsWith("-shm")
