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
    alwaysGenerateTypesMatching.set(listOf("Cat"))
    packageNamesFromFilePaths()
    generateApolloMetadata.set(true)
    mapScalar("Date", "java.util.Date")
  }
}

dependencies {
  add("apolloServiceUsedCoordinates", project(":node1:impl"))
  add("apolloServiceUsedCoordinates", project(":node2:impl"))
}
