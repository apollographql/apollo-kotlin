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
    alwaysGenerateTypesMatching.set(emptyList())
    generateApolloMetadata.set(true)
    mapScalar("Date", "java.util.Date")
  }
}

dependencies {
  add("apolloServiceUsedCoordinates", project(":leaf"))
}
