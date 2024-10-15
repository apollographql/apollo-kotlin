#!/usr/bin/env kotlin

import java.io.File
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory

val BRANCH_NAME = "kotlin-nightlies"

fun bumpVersions() {
  val kotlinVersion =
    getLatestVersion("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev/org/jetbrains/kotlin/kotlin-stdlib/maven-metadata.xml", prefix = "2.1.0")
  val kspVersion =
    getLatestVersion("https://oss.sonatype.org/content/repositories/snapshots/com/google/devtools/ksp/com.google.devtools.ksp.gradle.plugin/maven-metadata.xml")
  File("gradle/libraries.toml").let { file ->
    file.writeText(
        file.readText()
            .replaceVersion("kotlin-plugin", kotlinVersion)
            .replaceVersion("kotlin-plugin-max", kotlinVersion)
            .replaceVersion("ksp", kspVersion)
    )
  }
}

fun String.replaceVersion(key: String, version: String): String {
  return replace(Regex("""$key = ".*""""), """$key = "$version"""")
}

fun getLatestVersion(url: String, prefix: String? = null): String {
  val document = URL(url)
      .openConnection()
      .getInputStream().use { inputStream ->
        DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(inputStream)
      }
  return if (prefix != null) {
    document
        .getElementsByTagName("version")
        .let {
          (0 until it.length)
              .map { i -> it.item(i).textContent }
              .filter { it.startsWith(prefix) }
              .first() // Assumes they are sorted by most recent first, which is true on Kotlin's repo, false on Sonatype
        }
  } else {
    document
        .getElementsByTagName("latest")
        .item(0)
        .textContent
  }
}

fun runCommand(vararg args: String): String {
  val builder = ProcessBuilder(*args)
      .redirectError(ProcessBuilder.Redirect.INHERIT)
  val process = builder.start()
  val output = process.inputStream.bufferedReader().readText().trim()
  val ret = process.waitFor()
  if (ret != 0) {
    throw Exception("command ${args.joinToString(" ")} failed:\n$output")
  }
  return output
}

fun runCommand(args: String): String {
  return runCommand(*args.split(" ").toTypedArray())
}

fun rebaseOnTopOfMain() {
  val firstCommitMessage = "Add Kotlin Dev and Maven Central Snapshots repositories"
  runCommand("git fetch origin main:main")
  val baseCommit = runCommand("git", "rev-parse", "HEAD^{/$firstCommitMessage}^")
  runCommand("git rebase --rebase-merges $baseCommit --onto main")
}

fun triggerPrWorkflow() {
  runCommand("gh workflow run pr --ref $BRANCH_NAME")
}

fun commitAndPush() {
  val status = runCommand("git status")
  if (status.contains("nothing to commit")) {
    println("No changes to commit")
  } else {
    runCommand("git add .")
    runCommand("git", "commit", "-m", "Bump Kotlin and KSP")
  }
  runCommand("git push --force origin $BRANCH_NAME")
}

fun main() {
  println("Rebase on top of main")
  rebaseOnTopOfMain()

  println("Bump versions in libraries.toml")
  bumpVersions()

  println("Commit and push")
  commitAndPush()

  println("Trigger 'pr' workflow")
  triggerPrWorkflow()
}

main()
