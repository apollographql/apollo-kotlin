package com.apollographql.ijplugin.normalizedcache

import com.android.ddmlib.AndroidDebugBridge
import com.android.ddmlib.IDevice
import com.android.ddmlib.SyncService
import com.android.tools.idea.adb.AdbShellCommandsUtil
import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.util.logd
import com.apollographql.ijplugin.util.logw
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import java.io.File
import java.util.concurrent.TimeUnit

val isAndroidPluginPresent = try {
  Class.forName("com.android.ddmlib.AndroidDebugBridge")
  true
} catch (e: ClassNotFoundException) {
  false
}

@Suppress("LABEL_NAME_CLASH")
fun createPullFromDeviceActionGroup(project: Project, onFilePullError: (Throwable) -> Unit, onFilePulled: (File) -> Unit): ActionGroup {
  return DefaultActionGroup().apply {
    try {
      val adb = AndroidDebugBridge.createBridge(1, TimeUnit.SECONDS)
      if (adb.devices.isEmpty()) {
        add(DisabledAction(ApolloBundle.message("normalizedCacheViewer.pullFromDevice.noDevices")))
        return@apply
      }
      for (device in adb.devices) {
        add(DefaultActionGroup.createPopupGroup { device.name }.apply {
          if (device.clients.isEmpty()) {
            add(DisabledAction(ApolloBundle.message("normalizedCacheViewer.pullFromDevice.noRunningApp")))
            return@apply
          }
          for (client in device.clients) {
            val appPackageName = client.clientData.clientDescription
            add(DefaultActionGroup.createPopupGroup { appPackageName }.apply {
              val databasesDir = client.clientData.dataDir + "/databases"
              val commandResult = AdbShellCommandsUtil.create(device).executeCommandBlocking("run-as $appPackageName ls $databasesDir")
              if (commandResult.isError) {
                add(DisabledAction(ApolloBundle.message("normalizedCacheViewer.pullFromDevice.listDatabases.error")))
                return@apply
              }
              val databaseFileNames = commandResult.output.filter { it.endsWith(".db") }
              if (databaseFileNames.isEmpty()) {
                add(DisabledAction(ApolloBundle.message("normalizedCacheViewer.pullFromDevice.listDatabases.noDatabases")))
                return@apply
              }
              for (databaseFileName in databaseFileNames) {
                add(DumbAwareAction.create(databaseFileName) {
                  object : Task.Backgroundable(
                      project,
                      ApolloBundle.message("normalizedCacheViewer.pullFromDevice.pulling"),
                      false,
                  ) {
                    override fun run(indicator: ProgressIndicator) {
                      val pullResult = pullFile(device = device, appPackageName = appPackageName, databasesDir = databasesDir, databaseFileName = databaseFileName)
                      invokeLater {
                        pullResult.onSuccess {
                          onFilePulled(it)
                        }.onFailure {
                          logw(it, "Pull failed")
                          onFilePullError(it)
                        }
                      }
                    }
                  }.queue()
                })
              }
            })
          }
        })
      }
    } catch (e: Exception) {
      add(DisabledAction(ApolloBundle.message("normalizedCacheViewer.pullFromDevice.list.error", e.message ?: "")))
    }
  }
}

private fun pullFile(device: IDevice, appPackageName: String, databasesDir: String, databaseFileName: String): Result<File> {
  val localFile = File.createTempFile(databaseFileName.substringBeforeLast("."), ".db")
  val remoteFilePath = "$databasesDir/$databaseFileName"
  logd("Pulling $remoteFilePath to ${localFile.absolutePath}")
  val intermediateRemoteFilePath = "/data/local/tmp/${localFile.name}"
  val shellCommandsUtil = AdbShellCommandsUtil.create(device)
  var commandResult = shellCommandsUtil.executeCommandBlocking("touch $intermediateRemoteFilePath")
  if (commandResult.isError) {
    return Result.failure(Exception("'touch' command failed"))
  }
  commandResult = shellCommandsUtil.executeCommandBlocking("run-as $appPackageName sh -c 'cp $remoteFilePath $intermediateRemoteFilePath'")
  if (commandResult.isError) {
    return Result.failure(Exception("'copy' command failed"))
  }
  return runCatching {
    try {
      device.syncService.pullFile(intermediateRemoteFilePath, localFile.absolutePath, NullSyncProgressMonitor)
    } finally {
      commandResult = shellCommandsUtil.executeCommandBlocking("rm $intermediateRemoteFilePath")
      if (commandResult.isError) {
        logw("rm failed")
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

private class DisabledAction(text: String) : DumbAwareAction(text) {
  override fun actionPerformed(e: AnActionEvent) {}
  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = false
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}
