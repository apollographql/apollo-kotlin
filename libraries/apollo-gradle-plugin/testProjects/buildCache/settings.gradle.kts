rootProject.name = "buildCache"

include(":module:build.gradle.kts")
buildCache {
  local {
    directory = "../buildCache"
  }
}

dependencyResolutionManagement {
  versionCatalogs {
    create("libs") {
      from(files("../../../../../gradle/libraries.toml"))
    }
  }
}
