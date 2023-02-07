plugins {
  id("org.jetbrains.kotlin.jvm")
  alias(libs.plugins.apollo)
}

dependencies {
  implementation(kotlin("stdlib"))
  implementation(libs.apollo.api)
  testImplementation(libs.kotlin.test.junit)

  api(project(":root"))
}

apollo {
  service("service") {
    packageNamesFromFilePaths()
    dependsOn(project(":root"))
    isADependencyOf(project(":leaf"))
  }
}
