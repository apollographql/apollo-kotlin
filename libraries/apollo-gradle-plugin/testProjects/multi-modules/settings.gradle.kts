rootProject.name = "multi-modules"

include(":root", ":leaf")

dependencyResolutionManagement {
  versionCatalogs {
    create("libs") {
      from(files("../../../../gradle/libraries.toml"))
    }
  }
}
