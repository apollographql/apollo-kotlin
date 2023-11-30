plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation(libs.apollo.api)

  api(project(":root"))
}

apollo {
  service("service") {
    dependsOn(project(":root"))
    packageNamesFromFilePaths()
    generateApolloMetadata.set(true)
  }
}
