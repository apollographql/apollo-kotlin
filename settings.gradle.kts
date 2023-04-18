import com.gradle.enterprise.gradleplugin.internal.extension.BuildScanExtensionWithHiddenFeatures

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
  id("com.gradle.enterprise") version "3.13" // sync with libraries.toml
  id("com.gradle.common-custom-user-data-gradle-plugin") version "1.10"
  id("org.gradle.toolchains.foojay-resolver-convention") version "0.4.0"
}

apply(from = "./gradle/repositories.gradle.kts")

gradleEnterprise {
  server = "https://ge.apollographql.com"
  allowUntrustedServer = false

  buildScan {
    publishAlways()
    this as BuildScanExtensionWithHiddenFeatures
    publishIfAuthenticated()
    
    val isCiBuild = System.getenv("CI") != null
    isUploadInBackground = !isCiBuild

    capture {
      this.isTaskInputFiles = true
    }
  }
}
