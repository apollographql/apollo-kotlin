#!/usr/bin/env kotlin

import java.io.File

fun main() {
  val kotlinVersion = "2.0.20-Beta1"
  val kspVersion = "2.0.20-Beta1-1.0.22"

  val file = File("gradle/libraries.toml")
  val content = file.readText()
      .replaceVersion("kotlin-plugin", kotlinVersion)
      .replaceVersion("kotlin-plugin-max", kotlinVersion)
      .replaceVersion("kotlin-stdlib", kotlinVersion)
      .replaceVersion("ksp", kspVersion)
  file.writeText(content)
}

fun String.replaceVersion(key: String, version: String): String {
  return replace(Regex("""$key = ".*""""), """$key = "$version"""")
}

main()
