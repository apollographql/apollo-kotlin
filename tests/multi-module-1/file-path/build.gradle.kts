plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo")
}

apolloTest()

dependencies {
  implementation(libs.apollo.runtime)
  implementation(project(":multi-module-1-root"))
  testImplementation(libs.kotlin.test.junit)
}

apollo {
  service("service") {
    packageNamesFromFilePaths()
    generateApolloMetadata.set(true)
    alwaysGenerateTypesMatching.set(emptyList())
  }
}

dependencies {
  add("apolloService", project(":multi-module-1-root"))
}
