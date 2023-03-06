apply(from = "../../gradle/repositories.gradle.kts")

listOf(pluginManagement.repositories, dependencyResolutionManagement.repositories).forEach {
  it.apply {
    maven {
      url = uri("../../build/localMaven")
    }
  }
}

dependencyResolutionManagement {
  versionCatalogs {
    create("benchmarks") {
      from(files("../gradle/benchmarks.versions.toml"))
    }
    create("libs") {
      from(files("../../gradle/libraries.toml"))
    }
  }
}