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
    isADependencyOf(project(":node1"))
    isADependencyOf(project(":node2"))
    generateApolloMetadata.set(true)
    mapScalar("Date", "java.util.Date")
  }
}
