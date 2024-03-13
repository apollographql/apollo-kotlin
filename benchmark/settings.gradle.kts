pluginManagement {
  includeBuild("build-logic")
}
plugins {
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

listOf(pluginManagement.repositories, dependencyResolutionManagement.repositories).forEach {
  it.apply {
    maven {
      url = uri("../build/localMaven")
    }
  }
}

includeBuild("../")