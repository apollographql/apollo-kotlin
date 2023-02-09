// TODO: See https://youtrack.jetbrains.com/issue/KT-56536
//rootProject.name = "apollo-kotlin"

rootProject.projectDir
    .resolve("libraries")
    .listFiles()!!
    .filter { it.isDirectory }
    .filter { it.name.startsWith("apollo-") }
    .filter { File(it, "build.gradle.kts").exists() }
    .forEach {
      include(it.name)
      project(":${it.name}").projectDir = it
    }

include(":intellij-plugin")

pluginManagement {
  includeBuild("build-logic")
}

apply(from = "./gradle/repositories.gradle.kts")
