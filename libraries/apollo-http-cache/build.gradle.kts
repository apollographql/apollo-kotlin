plugins {
  id("org.jetbrains.kotlin.jvm")
}

apolloLibrary(
  javaModuleName = "com.apollographql.apollo3.cache.http"
)

dependencies {
  api(libs.okhttp)
  api(project(":apollo-api"))
  api(project(":apollo-runtime"))
  implementation(libs.moshi)
  implementation(libs.kotlinx.datetime)

  testImplementation(project(":apollo-mockserver"))
  testImplementation(libs.kotlin.test.junit)
  testImplementation(libs.truth)
}
