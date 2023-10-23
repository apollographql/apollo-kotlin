package com.apollographql.ijplugin.normalizedcache

import android.annotation.SuppressLint
import com.android.ddmlib.AndroidDebugBridge
import com.android.ddmlib.IDevice
import com.android.ddmlib.SyncService
import com.android.tools.idea.adb.AdbShellCommandResult
import com.android.tools.idea.adb.AdbShellCommandsUtil
import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.util.logd
import com.apollographql.ijplugin.util.logw
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.UpdateSession
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAware
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

fun createPullFromDeviceActionGroup(project: Project, onFilePullError: (Throwable) -> Unit, onFilePulled: (File) -> Unit): ActionGroup {
  return DefaultActionGroup().apply {
    try {
      val adb = AndroidDebugBridge.createBridge(1, TimeUnit.SECONDS)
      if (adb.devices.isEmpty()) {
        add(DisabledAction(ApolloBundle.message("normalizedCacheViewer.pullFromDevice.listDevices.empty")))
        return@apply
      }
      for (device in adb.devices) {
        add(DefaultActionGroup.createPopupGroup { device.name }.apply {
          // Add running apps
          for (client in device.clients) {
            if (!client.isValid) continue
            val appPackageName = client.clientData.clientDescription
            val databasesDir = client.clientData.dataDir + "/databases"
            add(PackageDatabasesActionGroup(project, device, appPackageName, databasesDir, onFilePulled, onFilePullError))
          }
          addSeparator()
          // Add other debuggable apps
          add(DebuggablePackagesActionGroup(project, device, onFilePulled, onFilePullError))
        })
      }
    } catch (e: Exception) {
      logd(e, "Could not list devices")
      add(DisabledAction(ApolloBundle.message("normalizedCacheViewer.pullFromDevice.listDevices.error", e.message?.let { ": $it" } ?: "")))
    }
  }
}

private class PackageDatabasesActionGroup(
    private val project: Project,
    private val device: IDevice,
    private val appPackageName: String,
    private val databasesDir: String,
    private val onFilePulled: (File) -> Unit,
    private val onFilePullError: (Throwable) -> Unit,
) : DefaultActionGroup(appPackageName, true), DumbAware {
  init {
    templatePresentation.isDisableGroupIfEmpty = false
  }

  val databaseFilesResult: Result<AdbShellCommandResult> by lazy {
    runCatching {
      AdbShellCommandsUtil.create(device).executeCommandBlocking(
          "run-as $appPackageName ls $databasesDir"
      )
    }
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  override fun getChildren(e: AnActionEvent?) = arrayOf(LoadingAction)

  override fun postProcessVisibleChildren(visibleChildren: List<AnAction>, updateSession: UpdateSession): List<AnAction> {
    if (databaseFilesResult.isFailure) {
      return listOf(DisabledAction(ApolloBundle.message("normalizedCacheViewer.pullFromDevice.listDatabases.error")))
    }
    val result = databaseFilesResult.getOrThrow()
    if (result.isError) {
      return listOf(DisabledAction(ApolloBundle.message(
          if (result.output.any { it.contains("No such file or directory") }) {
            "normalizedCacheViewer.pullFromDevice.listDatabases.empty"
          } else {
            "normalizedCacheViewer.pullFromDevice.listDatabases.error"
          }
      )))
    }
    val databaseFileNames = result.output.filter { it.endsWith(".db") }
    if (databaseFileNames.isEmpty()) {
      return listOf(DisabledAction(ApolloBundle.message("normalizedCacheViewer.pullFromDevice.listDatabases.empty")))
    }
    return databaseFileNames.map { databaseFileName ->
      DumbAwareAction.create(databaseFileName) {
        object : Task.Backgroundable(
            project,
            ApolloBundle.message("normalizedCacheViewer.pullFromDevice.pull.ongoing"),
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
      }
    }
  }
}

private class DebuggablePackagesActionGroup(
    private val project: Project,
    private val device: IDevice,
    private val onFilePulled: (File) -> Unit,
    private val onFilePullError: (Throwable) -> Unit,
) : DefaultActionGroup(@Suppress("DialogTitleCapitalization") ApolloBundle.message("normalizedCacheViewer.pullFromDevice.listDebuggablePackages.title"), true), DumbAware {
  init {
    templatePresentation.isDisableGroupIfEmpty = false
  }

  private val debuggablePackagesResult: Result<AdbShellCommandResult> by lazy {
    runCatching {
      AdbShellCommandsUtil.create(device).executeCommandBlocking(
          // List all packages, and try run-as on them - if it succeeds, the package is debuggable
          "for p in \$(pm list packages -3 | cut -d : -f 2); do (run-as \$p true >/dev/null 2>&1 && echo \$p); done; true"
      )
    }
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  override fun getChildren(e: AnActionEvent?) = arrayOf(LoadingAction)

  @SuppressLint("SdCardPath")
  override fun postProcessVisibleChildren(visibleChildren: List<AnAction>, updateSession: UpdateSession): List<AnAction> {
    if (debuggablePackagesResult.isFailure) {
      return listOf(DisabledAction(ApolloBundle.message("normalizedCacheViewer.pullFromDevice.listDebuggablePackages.error")))
    }
    val result = debuggablePackagesResult.getOrThrow()
    if (result.isError) {
      return listOf(DisabledAction(ApolloBundle.message("normalizedCacheViewer.pullFromDevice.listDebuggablePackages.error")))
    }
    val packageNames = result.output.filterNot { it.isEmpty() }
    if (packageNames.isEmpty()) {
      return listOf(DisabledAction(ApolloBundle.message("normalizedCacheViewer.pullFromDevice.listDebuggablePackages.empty")))
    }
    return packageNames.map { packageName ->
      PackageDatabasesActionGroup(project, device, packageName, "/data/data/$packageName/databases", onFilePulled, onFilePullError)
    }
  }
}

private fun pullFile(device: IDevice, appPackageName: String, databasesDir: String, databaseFileName: String): Result<File> {
  val localFile = File.createTempFile(databaseFileName.substringBeforeLast("."), ".db")
  val remoteFilePath = "$databasesDir/$databaseFileName"
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

private class DisabledAction(text: String) : AnAction(text) {
  init {
    templatePresentation.isEnabled = false
  }

  override fun actionPerformed(e: AnActionEvent) {}

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}

private val LoadingAction = DisabledAction(ApolloBundle.message("normalizedCacheViewer.pullFromDevice.loading"))
