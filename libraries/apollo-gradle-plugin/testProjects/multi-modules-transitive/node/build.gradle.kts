plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation(libs.apollo.api)

  api(project(":root"))
  apolloMetadata(project(":root"))
}

apollo {
  packageNamesFromFilePaths()
  generateApolloMetadata.set(true)
}
