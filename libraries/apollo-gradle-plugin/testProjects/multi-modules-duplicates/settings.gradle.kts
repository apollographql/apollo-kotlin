rootProject.name = "multi-modules"

include(":root", ":node1", ":node2")

dependencyResolutionManagement {
  versionCatalogs {
    create("libs") {
      from(files("../../../../gradle/libraries.toml"))
    }
  }
}
