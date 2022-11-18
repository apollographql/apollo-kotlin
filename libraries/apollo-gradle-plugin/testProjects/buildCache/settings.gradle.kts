rootProject.name = "buildCache"

apply(from = "../../../../gradle/test.settings.gradle.kts")

buildCache {
  local {
    directory = "../testProjectBuildCache"
  }
}

