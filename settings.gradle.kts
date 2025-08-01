plugins {
  id("com.gradle.develocity") version "4.1" // sync with libraries.toml
  id("com.gradle.common-custom-user-data-gradle-plugin") version "2.3"
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
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

dependencyResolutionManagement {
  versionCatalogs {
    create("libs") {
      from(files("gradle/libraries.toml"))
    }
  }
}

includeBuild("build-logic")

apply(from = "./gradle/repositories.gradle.kts")
apply(from = "./gradle/ge.gradle")
