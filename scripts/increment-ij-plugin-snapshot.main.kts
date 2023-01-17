#!/usr/bin/env kotlin
import java.io.File

incrementSnapshotVersion()
runCommand("bash", "-c", "./gradlew :intellij-plugin:updatePluginsXml")
runCommand("git", "add", "intellij-plugin/gradle.properties")
runCommand("git", "add", "intellij-plugin/snapshots/plugins.xml")
runCommand("git", "commit", "-m", "Increment IJ plugin snapshot version")

fun incrementSnapshotVersion() {
  val ijPropertiesFile = File("intellij-plugin/gradle.properties")
  val ijPropertiesText = ijPropertiesFile.readText()
  val updatedIjPropertiesText = ijPropertiesText.replace(Regex("snapshotVersion=(\\d+)")) {
    val version = it.groupValues[1].toInt()
    "snapshotVersion=${version + 1}"
  }
  ijPropertiesFile.writeText(updatedIjPropertiesText)
}

fun runCommand(vararg args: String): String {
  val builder = ProcessBuilder(*args)
      .redirectError(ProcessBuilder.Redirect.INHERIT)

  val process = builder.start()
  val ret = process.waitFor()

  val output = process.inputStream.bufferedReader().readText()
  if (ret != 0) {
    throw java.lang.Exception("command ${args.joinToString(" ")} failed:\n$output")
  }

  return output
}

