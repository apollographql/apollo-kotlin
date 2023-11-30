plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation(kotlin("stdlib"))
  implementation(libs.apollo.api)
  testImplementation(libs.kotlin.test.junit)

  api(project(":root"))
}

apollo {
  service("service") {
    dependsOn(project(":root"))
    packageNamesFromFilePaths()
    generateApolloMetadata.set(true)
  }
}
