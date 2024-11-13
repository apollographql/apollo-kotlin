pluginManagement {
  includeBuild("build-logic")
}
plugins {
  id("com.gradle.develocity") version "3.18.2" // sync with libraries.toml
  id("com.gradle.common-custom-user-data-gradle-plugin") version "2.0.2"
  id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

include(":app", ":microbenchmark", ":macrobenchmark")

dependencyResolutionManagement {
  versionCatalogs {
    create("libs") {
      from(files("../gradle/libraries.toml"))
    }
  }
}

apply(from = "../gradle/repositories.gradle.kts")
apply(from = "../gradle/ge.gradle")

listOf(pluginManagement.repositories, dependencyResolutionManagement.repositories).forEach {
  it.apply {
    maven {
      url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    }
  }
}

includeBuild("../")
