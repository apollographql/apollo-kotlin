plugins {
  id("org.jetbrains.kotlin.jvm")
  alias(libs.plugins.apollo)
  id("maven-publish")
}

dependencies {
  implementation(kotlin("stdlib"))
  implementation(libs.apollo.api)

  testImplementation(libs.kotlin.test.junit)
}

apollo {
  service("service") {
    packageNamesFromFilePaths()
    // PLACEHOLDER
    generateApolloMetadata.set(true)
    isADependencyOf(project(":leaf"))
    mapScalar("Date", "java.util.Date")
  }
}
