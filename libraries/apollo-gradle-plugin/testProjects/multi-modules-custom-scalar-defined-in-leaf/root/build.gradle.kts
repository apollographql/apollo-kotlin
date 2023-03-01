plugins {
  id("org.jetbrains.kotlin.jvm")
  alias(libs.plugins.apollo)
}

dependencies {
  implementation(kotlin("stdlib"))
  implementation(libs.apollo.api)

  testImplementation(libs.kotlin.test.junit)
}

apollo {
  service("service") {
    packageName.set("com.library")
    generateApolloMetadata.set(true)
    mapScalar("Date", "java.util.Date")
  }
}
