plugins {
  id("org.jetbrains.kotlin.jvm")
  alias(libs.plugins.apollo)
}

dependencies {
  implementation(libs.apollo.api)
  implementation(project(":root"))
}

apollo {
  service("service") {
    // PLACEHOLDER
    packageNamesFromFilePaths()
    dependsOn(project(":root"))
  }
}
