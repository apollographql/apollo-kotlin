#!/usr/bin/env kotlin

import java.io.File
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory

fun main() {
  // Update versions in libraries.toml
  val kotlinVersion = getLatestVersion("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev/org/jetbrains/kotlin/kotlin-stdlib/maven-metadata.xml", prefix = "2.0.20")
  val kspVersion = getLatestVersion("https://oss.sonatype.org/content/repositories/snapshots/com/google/devtools/ksp/com.google.devtools.ksp.gradle.plugin/maven-metadata.xml")
  File("gradle/libraries.toml").let { file ->
    file.writeText(
        file.readText()
            .replaceVersion("kotlin-plugin", kotlinVersion)
            .replaceVersion("kotlin-plugin-max", kotlinVersion)
            .replaceVersion("kotlin-stdlib", kotlinVersion)
            .replaceVersion("ksp", kspVersion)
    )
  }

  // Uncomment Kotlin dev maven repository, add Sonatype snapshots repository
  setOf(
      File("gradle/repositories.gradle.kts"),
      File("intellij-plugin/build.gradle.kts"),
  )
      .forEach { file ->
        file.writeText(
            file.readText()
                .replace(
                    """// maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev/") }""",
                    """
                      maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev/") }
                      maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
                    """.trimIndent()
                )
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

main()
