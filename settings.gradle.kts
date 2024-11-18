pluginManagement {
  includeBuild("build-logic")
}

plugins {
  id("com.gradle.develocity") version "3.18.2" // sync with libraries.toml
  id("com.gradle.common-custom-user-data-gradle-plugin") version "2.0.2"
  id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

val javaVersion: String = System.getProperty("java.version")
if (javaVersion.substringBefore(".").toInt() < 17) {
  throw GradleException("Java 17 or higher is required to build this project. You are using Java $javaVersion.")
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

