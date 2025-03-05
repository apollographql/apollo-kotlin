plugins {
  id("org.jetbrains.kotlin.jvm")
  alias(libs.plugins.apollo)
}

dependencies {
  implementation(libs.apollo.api)

  api(project(":root"))
}

apollo {
  service("service") {
    packageNamesFromFilePaths()
    generateApolloMetadata.set(true)
    alwaysGenerateTypesMatching.set(emptyList())
  }
}

dependencies {
  add("apolloService", project(":root"))
  add("apolloServiceUsedCoordinates", project(":leaf"))
}
