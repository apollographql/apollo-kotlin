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
    packageName.set("com.library")
    alwaysGenerateTypesMatching.set(emptyList())
    generateApolloMetadata.set(true)
    mapScalar("Date", "java.util.Date")
    mapScalar("ID", "com.library.MyID", "com.library.MyIDAdapter()")
  }
}

dependencies {
  add("apolloServiceUsedCoordinates", project(":leaf"))
}
