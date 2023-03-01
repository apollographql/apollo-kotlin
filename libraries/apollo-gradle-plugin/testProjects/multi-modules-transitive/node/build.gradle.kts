plugins {
  id("org.jetbrains.kotlin.jvm")
  alias(libs.plugins.apollo)
}

dependencies {
  implementation(libs.apollo.api)

  api(project(":root"))
}

apollo {
  service("service") {
    packageNamesFromFilePaths()
    isADependencyOf(project(":leaf"))
    dependsOn(project(":root"))
  }
}
