pluginManagement {
  includeBuild("build-logic")
}

plugins {
  id("com.gradle.enterprise") version "3.13.4" // sync with libraries.toml
  id("com.gradle.common-custom-user-data-gradle-plugin") version "1.11"
  id("org.gradle.toolchains.foojay-resolver-convention") version "0.6.0"
}

rootProject.name = "apollo-kotlin"

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

dependencyResolutionManagement {
  versionCatalogs {
    create("libs") {
      from(files("gradle/libraries.toml"))
    }
  }
}

apply(from = "./gradle/repositories.gradle.kts")
apply(from = "./gradle/ge.gradle")
