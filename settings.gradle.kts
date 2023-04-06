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

pluginManagement {
  includeBuild("build-logic")
}

plugins {
  id("com.gradle.enterprise") version "3.12.4"
  id("org.gradle.toolchains.foojay-resolver-convention") version "0.4.0"
}

apply(from = "./gradle/repositories.gradle.kts")

gradleEnterprise {
  server = "https://ge.apollographql.com"

  buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"

    val isCiBuild = System.getenv("CI") != null
    isUploadInBackground = !isCiBuild
  }
}
