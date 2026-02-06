#!/usr/bin/env kotlin

@file:DependsOn("com.github.zafarkhaja:java-semver:0.10.2")

import com.github.zafarkhaja.semver.Version
import java.io.File
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory

val BRANCH_NAME = "kotlin-nightlies"

fun bumpVersions() {
  val kotlinVersion =
    getLatestVersion("https://redirector.kotlinlang.org/maven/dev/org/jetbrains/kotlin/kotlin-stdlib/maven-metadata.xml", prefix = "2.3.20")

  val useKspSnapshots = true
  val kspVersion = getLatestVersion(
      if (useKspSnapshots) {
        "https://central.sonatype.com/repository/maven-snapshots/com/google/devtools/ksp/com.google.devtools.ksp.gradle.plugin/maven-metadata.xml"
      } else {
        "https://repo1.maven.org/maven2/com/google/devtools/ksp/com.google.devtools.ksp.gradle.plugin/maven-metadata.xml"
      }
  )

  File("gradle/libraries.toml").let { file ->
    file.writeText(
        file.readText()
            .replaceVersion("kotlin-plugin", kotlinVersion)
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
          @Suppress("SimplifiableCallChain")
          (0 until it.length)
              .map { i -> it.item(i).textContent }
              .filter { it.startsWith(prefix) }
              .sortedBy {
                Version.parse(
                    // Make it SemVer comparable
                    it
                        .replace("-dev-", "-Dev.")
                        .replace("-RC-", "-RC.")
                        .replace("-RC2-", "-RC2.")
                        .replace("-RC3-", "-RC3.")
                        .replace("-Beta-", "-Beta.")
                        .replace("-Beta2-", "-Beta2.")
                        .replace("-Beta3-", "-Beta3.")
                )
              }
              .last()
        }
  } else {
    document
        .getElementsByTagName("latest")
        .item(0)
        .textContent
  }
}

fun runCommand(vararg args: String): String {
  println("Running command: '${args.joinToString(" ")}'")
  val builder = ProcessBuilder(*args)
      .redirectErrorStream(true)
      .redirectError(ProcessBuilder.Redirect.INHERIT)
  val process = builder.start()
  val output = StringBuilder()
  val reader = process.inputStream.bufferedReader()
  while (true) {
    val line = reader.readLine() ?: break
    println("> $line")
    output.append(line + "\n")
  }
  val ret = process.waitFor()
  if (ret != 0) {
    throw Exception("command ${args.joinToString(" ")} failed (ret=$ret):\n$output")
  }
  return output.toString().trim()
}

fun runCommand(args: String): String {
  return runCommand(*args.split(" ").toTypedArray())
}

fun rebaseOnTopOfMain() {
  val firstCommitMessage = "Add Kotlin Dev and Maven Central Snapshots repositories"
  runCommand("git fetch origin main:main")
  val baseCommit = runCommand("git", "rev-parse", "HEAD^{/$firstCommitMessage}^")
  println("Base commit: $baseCommit")
  runCommand("git rebase --rebase-merges $baseCommit --onto main")
}

fun triggerPrWorkflow() {
  runCommand("gh workflow run build-pull-request --ref $BRANCH_NAME")
}

fun updateLockFiles() {
  File("kotlin-js-store").deleteRecursively()
  File("tests/kotlin-js-store").deleteRecursively()
  runCommand("./gradlew kotlinUpgradePackageLock kotlinWasmUpgradePackageLock")
  runCommand("./gradlew -p tests kotlinUpgradePackageLock kotlinWasmUpgradePackageLock")
}

fun hasChanges(): Boolean {
  return try {
    runCommand("git diff --quiet")
    false
  } catch (_: Exception) {
    true
  }
}

fun commitAndPush() {
  if (!hasChanges()) {
    println("No changes to commit")
  } else {
    runCommand("git add .")
    runCommand("git", "commit", "-m", "Bump Kotlin and KSP")
    runCommand("git push --force origin $BRANCH_NAME")
  }
}

fun main() {
  println("Rebase on top of main")
  rebaseOnTopOfMain()

  println("Bump versions in libraries.toml")
  bumpVersions()

  println("Update lock files")
  updateLockFiles()

  println("Commit and push")
  commitAndPush()

  println("Trigger 'build-pull-request' workflow")
  triggerPrWorkflow()
}

main()
