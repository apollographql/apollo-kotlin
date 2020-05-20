rootProject.name = "buildCache"

include(":module:build.gradle.kts")
buildCache {
  local {
    directory = "../buildCache"
  }
}
