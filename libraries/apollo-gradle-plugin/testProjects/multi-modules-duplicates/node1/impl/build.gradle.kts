plugins {
  id("org.jetbrains.kotlin.jvm")
  alias(libs.plugins.apollo)
}

dependencies {
  implementation(kotlin("stdlib"))
  implementation(libs.apollo.api)
  testImplementation(libs.kotlin.test.junit)

  implementation(project(":root"))
}

apollo {
  service("service") {
    packageNamesFromFilePaths()
    alwaysGenerateTypesMatching.set(emptyList())
    generateApolloMetadata.set(true)
  }
}


dependencies {
  add("apolloService", project(":root"))
}

