plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo3")
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
