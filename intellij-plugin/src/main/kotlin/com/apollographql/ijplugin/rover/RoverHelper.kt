package com.apollographql.ijplugin.rover

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.processTools.getResultStdoutStr
import com.intellij.execution.processTools.mapFlat
import kotlinx.coroutines.runBlocking

object RoverHelper {
  private fun getRoverBinDirectory() = "${System.getProperty("user.home")}/.rover/bin"

  private fun getRoverExecutablePath() = "${getRoverBinDirectory()}/rover"

  fun getLspCommandLine() = object : GeneralCommandLine("rover", "lsp") {
    override fun createProcess(processBuilder: ProcessBuilder): Process {
      return try {
        super.createProcess(processBuilder)
      } catch (e: Exception) {
        throw IllegalStateException(
            "\n\nFailed to start the Apollo LSP server. Please make sure you have the latest version of `rover` installed.\n" +
                "Installation instructions are available at https://www.apollographql.com/docs/rover/getting-started\n",
            e,
        )
      }
    }
  }
      .withExePath(getRoverExecutablePath())
      .withWorkDirectory(getRoverBinDirectory())
      .withEnvironment("RUST_BACKTRACE", "full")

  sealed interface RoverStatus {
    data object NotInstalled : RoverStatus
    class Installed(val version: String) : RoverStatus {
      val hasLsp: Boolean
        get() {
          // Need at least 0.27.0
          val (major, minor) = version.split('.').take(2).map { it.toIntOrNull() ?: 0 }
          return major > 0 || minor >= 27
        }
    }
  }

  fun getRoverStatus(): RoverStatus {
    val result: Result<String> = runBlocking {
      kotlin.runCatching {
        GeneralCommandLine("rover", "--version")
            .withExePath(getRoverExecutablePath())
            .withWorkDirectory(getRoverBinDirectory())
            .createProcess()
      }.mapFlat {
        it.getResultStdoutStr()
      }
    }
    return result.getOrNull()?.let { RoverStatus.Installed(it.split(' ').getOrNull(1) ?: it) } ?: RoverStatus.NotInstalled
  }
}
