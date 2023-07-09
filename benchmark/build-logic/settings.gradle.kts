apply(from = "../../gradle/repositories.gradle.kts")

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
