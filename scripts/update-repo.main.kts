#!/usr/bin/env kotlin

@file:Repository("https://repo.maven.apache.org/maven2/")
@file:Repository("file://~/.m2/repository")
@file:Repository("https://dl.google.com/android/maven2/")
@file:Repository("https://storage.googleapis.com/gradleup/m2")

@file:DependsOn("com.gradleup.librarian:librarian-cli:0.2.2-SNAPSHOT")

import com.gradleup.librarian.repo.*
import com.gradleup.librarian.cli.command.VersionContext
import java.io.File


fun VersionContext.setVersion() {
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

fun VersionContext.setVersionInDocs() {
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

fun getVersion(): String {
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

updateRepo(::getVersion, VersionContext::setVersion, VersionContext::setVersionInDocs)
