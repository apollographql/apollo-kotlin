#!/usr/bin/env kotlin
import java.io.File

/**
 * A script to generate apollo-rx3-support automatically from apollo-rx2-support
 * You need kotlin 1.3.70+ installed on your machine
 */

check(File("settings.gradle.kts").let { it.exists() && it.readText().contains("rootProject.name = \"apollo-android\"") }) {
  "This script needs to be called from the root of the project"
}

val rx3Dir = File("apollo-rx3-support")
rx3Dir.deleteRecursively()
File("apollo-rx2-support").copyRecursively(rx3Dir, overwrite = true)


rx3Dir.walk().filter { it.name == "rx2" }.forEach {
  it.renameTo(File(it.parentFile, "rx3"))
}

rx3Dir.walk().filter { it.name.contains("Rx2") }.forEach {
  it.renameTo(File(it.parentFile, it.name.replace("Rx2", "Rx3")))
}

rx3Dir.walk().filter { it.isFile && (it.extension == "kt" || it.extension == "properties" || it.extension == "kts") }.forEach {
  val header = """
    |/*
    | * This file is auto generated from apollo-rx2-support by rxjava3.main.kts, do not edit manually.
    | */
    |
  """.trimMargin()
  it.writeText(
      header + it.readText()
          .replace("com.apollographql.apollo3.rx2", "com.apollographql.apollo3.rx3")
          .replace("Rx2", "Rx3")
          .replace("rx2", "rx3")
          .replace("RxJava2", "RxJava3")
          .replace("import io.reactivex.annotations", "import io.reactivex.rxjava3.annotations")
          .replace("import io.reactivex.Flowable", "import io.reactivex.rxjava3.core.Flowable")
          .replace("import io.reactivex.Scheduler", "import io.reactivex.rxjava3.core.Scheduler")
          .replace("import io.reactivex.Single", "import io.reactivex.rxjava3.core.Single")
          .replace("import io.reactivex.schedulers.Schedulers", "import io.reactivex.rxjava3.schedulers.Schedulers")
  )
}

