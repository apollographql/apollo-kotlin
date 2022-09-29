plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation(kotlin("stdlib"))
  implementation(libs.apollo.api)

  testImplementation(libs.kotlin.test.junit)
}

apollo {
  packageName.set("com.library")
  generateApolloMetadata.set(true)
  mapScalar("Date", "java.util.Date")
}
