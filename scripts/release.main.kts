#!/usr/bin/env kotlin
import java.io.File
import kotlin.system.exitProcess

/**
 * A script to run locally in order to make a release.
 *
 * You need kotlin 1.3.70+ and gh installed on your machine
 */


if (runCommand("git", "status", "--porcelain").isNotEmpty()) {
  println("Your git repo is not clean. Make sur to stash or commit your changes before making a release")
  exitProcess(1)
}

check(getCurrentVersion().endsWith("-SNAPSHOT")) {
  "Version '${getCurrentVersion()} is not a -SNAPSHOT, check your working directory"
}

check(args.size == 1) {
  "release.main.kts [version to release]"
}
val versionToRelease = args[0]
val nextSnapshot = getNextSnapshot(versionToRelease)

val startBranch = runCommand("git", "symbolic-ref", "--short", "HEAD")
check(startBranch == "main" || startBranch.startsWith("release-")) {
  "You must be on the main branch or a release branch to make a release"
}

while (true) {
  println("Current version is '${getCurrentVersion()}'.")
  println("Release '$versionToRelease' and bump to '$nextSnapshot' [y/n]?")

  when (readLine()!!.trim()) {
    "y" -> break
    "n" -> {
      println("Aborting.")
      exitProcess(1)
    }
  }
}

// 'De-snapshot' the version, open a PR, and merge it
val releaseBranchName = "release-$versionToRelease"
runCommand("git", "checkout", "-b", releaseBranchName)
setCurrentVersion(versionToRelease)
setVersionInDocs(versionToRelease, nextSnapshot)
setVersionInIntelliJPlugin(versionToRelease)
runCommand("git", "commit", "-a", "-m", "release $versionToRelease")
runCommand("git", "push", "origin", releaseBranchName)
runCommand("gh", "pr", "create", "--base", startBranch, "--fill")

println("Press enter to merge the release PR")
readLine()
mergeAndWait(releaseBranchName)
println("Release PR merged.")

// Tag the release, and push the tag
runCommand("git", "checkout", startBranch)
runCommand("git", "pull", "origin", startBranch)
val tagName = "v$versionToRelease"
runCommand("git", "tag", tagName)

println("Press enter to push the tag")
readLine()
runCommand("git", "push", "origin", tagName)
println("Tag pushed.")

// Bump the version to the next snapshot
val bumpVersionBranchName = "release-$versionToRelease-bump-snapshot"
runCommand("git", "checkout", "-b", bumpVersionBranchName)
setCurrentVersion(nextSnapshot)
runCommand("git", "commit", "-a", "-m", "version is now $nextSnapshot")
runCommand("git", "push", "origin", bumpVersionBranchName)
runCommand("gh", "pr", "create", "--base", startBranch, "--fill")

println("Press enter to merge the bump version PR")
readLine()
mergeAndWait(bumpVersionBranchName)
println("Bump version PR merged.")

// Go back and pull the changes
runCommand("git", "checkout", startBranch)
runCommand("git", "pull", "origin", startBranch)

println("Everything is done.")

fun runCommand(vararg args: String): String {
  val builder = ProcessBuilder(*args)
      .redirectError(ProcessBuilder.Redirect.INHERIT)

  val process = builder.start()
  val ret = process.waitFor()

  val output = process.inputStream.bufferedReader().readText()
  if (ret != 0) {
    throw java.lang.Exception("command ${args.joinToString(" ")} failed:\n$output")
  }

  return output.trim()
}

fun setCurrentVersion(version: String) {
  val gradleProperties = File("gradle.properties")
  var newContent = gradleProperties.readLines().map {
    it.replace(Regex("VERSION_NAME=.*"), "VERSION_NAME=$version")
  }.joinToString(separator = "\n", postfix = "\n")
  gradleProperties.writeText(newContent)

  val versionCatalog = File("gradle/libraries.toml")
  // apollo = "3.5.1"
  newContent = versionCatalog.readLines().map {
    it.replace(Regex("( *apollo *= *\").*(\".*)")) {
      "${it.groupValues[1]}$version${it.groupValues[2]}"
    }
  }.joinToString(separator = "\n", postfix = "\n")
  versionCatalog.writeText(newContent)
}

fun getCurrentVersion(): String {
  val versionLines = File("gradle.properties").readLines().filter { it.startsWith("VERSION_NAME=") }

  require(versionLines.size > 0) {
    "cannot find the version in ./gradle.properties"
  }

  require(versionLines.size == 1) {
    "multiple versions found in ./gradle.properties"
  }

  val regex = Regex("VERSION_NAME=(.*)-SNAPSHOT")
  val matchResult = regex.matchEntire(versionLines.first())

  require(matchResult != null) {
    "'${versionLines.first()}' doesn't match VERSION_NAME=(.*)-SNAPSHOT"
  }

  return matchResult.groupValues[1] + "-SNAPSHOT"
}

fun getNextSnapshot(version: String): String {
  val components = version.split(".").toMutableList()
  val part = components.removeLast()
  var digitCount = 0
  for (i in part.indices.reversed()) {
    if (part[i] < '0' || part[i] > '9') {
      break
    }
    digitCount++
  }

  check(digitCount > 0) {
    "Cannot find a number to bump in $version"
  }

  // prefix can be "alpha", "dev", etc...
  val prefix = if (digitCount < part.length) {
    part.substring(0, part.length - digitCount)
  } else {
    ""
  }
  val numericPart = part.substring(part.length - digitCount, part.length)
  val asNumber = numericPart.toInt()

  val nextPart = if (numericPart[0] == '0') {
    // https://docs.gradle.org/current/userguide/single_versions.html#version_ordering
    // Gradle understands that alpha2 > alpha10 but it might not be the case for everyone so
    // use the same naming schemes as other libs and keep the prefix
    val width = numericPart.length
    String.format("%0${width}d", asNumber + 1)
  } else {
    (asNumber + 1).toString()
  }

  components.add("$prefix$nextPart")
  return components.joinToString(".") + "-SNAPSHOT"
}

fun setVersionInDocs(version: String, nextSnapshot: String) {
  for (file in File("docs/source").walk() + File("README.md")) {
    if (file.isDirectory || !(file.name.endsWith(".md") || file.name.endsWith(".mdx"))) continue

    // Don't touch docs inside /migration
    if (file.path.contains("migration")) continue

    val content = file.readText()
        // Plugin
        .replace(Regex("""id\("(com\.apollographql\.apollo.?)"\) version "(.+)""")) {
          """
            id("${it.groupValues[1]}") version "$version"
          """.trimIndent()
        }
        // Dependencies
        .replace(Regex(""""(com\.apollographql\.apollo.?):(.+):.+"""")) {
          """"${it.groupValues[1]}:${it.groupValues[2]}:$version""""
        }
        // Tutorial
        .replace(Regex("This tutorial uses `(.+)`")) {
          "This tutorial uses `$version`"
        }
        // index.md
        .replace(Regex("The latest version is `(.+)`")) {
          "The latest version is `$version`"
        }
        // Snapshots
        .replace(Regex("And then use the `(.+)` version for the plugin and libraries.")) {
          "And then use the `$nextSnapshot` version for the plugin and libraries."
        }
    file.writeText(content)
  }
}

fun setVersionInIntelliJPlugin(version: String) {
  File("intellij-plugin/src/main/kotlin/com/apollographql/ijplugin/refactoring/migration/v3tov4/ApolloV3ToV4MigrationProcessor.kt").let { file ->
    file.writeText(file.readText().replace(Regex("""apollo4LatestVersion = "(.+)"""")) {
      """apollo4LatestVersion = "$version""""
    })
  }
  File("intellij-plugin/src/test/testData/migration/v3-to-v4/updateGradleDependenciesInLibsVersionsToml_after.versions.toml").let { file ->
    file.writeText(file.readText()
        .replace(Regex(""""com\.apollographql\.apollo:apollo-runtime:4(.+)"""")) {
          """"com.apollographql.apollo:apollo-runtime:$version""""
        }
        .replace(Regex(""""com\.apollographql\.apollo:4(.+)"""")) {
          """"com.apollographql.apollo:$version""""
        }
        .replace(Regex(""""4(.+)"""")) {
          """"$version""""
        }
    )
  }
  File("intellij-plugin/src/test/testData/migration/v3-to-v4/upgradeGradlePluginInBuildGradleKts_after.gradle.kts").let { file ->
    file.writeText(file.readText()
        .replace(Regex(""""4(.+)"""")) {
          """"$version""""
        }
        .replace(Regex("""// TODO: Update version to 4(.+)""")) {
          """// TODO: Update version to $version"""
        }
    )
  }
}

fun mergeAndWait(branchName: String) {
  runCommand("gh", "pr", "merge", branchName, "--squash", "--auto", "--delete-branch")
  println("Waiting for the PR to be merged...")
  while (true) {
    val state = runCommand("gh", "pr", "view", branchName, "--json", "state", "--jq", ".state")
    if (state == "MERGED") break
    Thread.sleep(1000)
  }
}
