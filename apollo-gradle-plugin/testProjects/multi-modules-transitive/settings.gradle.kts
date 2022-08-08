rootProject.name = "multi-modules"

include(":root", ":node", "leaf")

dependencyResolutionManagement {
  versionCatalogs {
    create("libs") {
      from(files("../../../gradle/libs.versions.toml"))
    }
  }
}
