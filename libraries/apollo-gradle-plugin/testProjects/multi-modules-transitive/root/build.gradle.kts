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
    generateApolloMetadata.set(true)
    alwaysGenerateTypesMatching.set(emptyList())
    mapScalar("Date", "java.util.Date")
  }
}

dependencies {
  add("apolloServiceUsedCoordinates", project(":node"))
}

