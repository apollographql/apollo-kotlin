apply(from = "../../gradle/repositories.gradle.kts")

dependencyResolutionManagement {
  versionCatalogs {
    create("libs") {
      from(files("../../gradle/libraries.toml"))
    }
  }
}
