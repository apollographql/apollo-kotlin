#!/usr/bin/env kotlin

@file:DependsOn("com.github.ajalt.clikt:clikt-jvm:5.0.2")


import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import java.io.File

class MainCommand : CliktCommand() {
  override fun run() {
  }
}

class PrepareNextVersion : CliktCommand() {
  override fun run() {
    val currentVersion = getCurrentVersion()
    check(currentVersion.endsWith("-SNAPSHOT")) {
      "Current version '$currentVersion' does not ends with '-SNAPSHOT'. Call set-version to update it."
    }

    val releaseVersion = currentVersion.dropSnapshot()
    val nextSnapshot = getNextSnapshot(releaseVersion)

    setVersionInDocs(releaseVersion)
    setCurrentVersion(nextSnapshot)

    println("Docs have been updated to use version '$releaseVersion'.")
    println("Version is now '$nextSnapshot'.")
  }
}

class SetVersion : CliktCommand() {
  val version by argument()
  override fun run() {
    setCurrentVersion(version)

    println("Version is now '$version'.")
  }
}

private fun String.dropSnapshot() = this.removeSuffix("-SNAPSHOT")

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

  for (file in File("tests/integration-tests").walk()) {
    if (file.isDirectory || !file.name.endsWith(".json")) continue
    val content = file.readText()
        .replace(Regex("""\{"name":"apollo-kotlin","version":"(.+)"\}""")) {
          """{"name":"apollo-kotlin","version":"$version"}"""
        }
    file.writeText(content)
  }
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

fun setVersionInDocs(version: String) {
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
    file.writeText(content)
  }
}

MainCommand().subcommands(PrepareNextVersion(), SetVersion()).main(args)