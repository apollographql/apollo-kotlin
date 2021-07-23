#!/usr/bin/env kotlin

import java.io.File

val currentDirectory = File(".").absoluteFile.parentFile
check(currentDirectory.name == "docs") {
  "This script must be called from the 'docs' folder (and not '${currentDirectory.name}')"
}

val rootProjectDir = File("..")

rootProjectDir.listFiles().filter { it.isDirectory && it.name.startsWith("apollo-") }
    .filter {
      File(it, "build/dokka/gfm").exists()
    }.forEach {
      installKdoc(it)
    }

fun installKdoc(srcProjectDir: File) {
  val dst = File("source/kdoc/${srcProjectDir.name}")

  val src = File(srcProjectDir, "build/dokka/gfm")

  dst.mkdirs()

  src.copyRecursively(dst, overwrite = true)

//  dst.walk().filter { it.isFile && it.extension == "md" }
//      .forEach {
//        it.replaceInText("/index.md", "/")
//      }
}

fun File.replaceInText(oldValue: String, newValue: String) {
  val text = readText()
  writeText(text.replace(oldValue, newValue))
}
