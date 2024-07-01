plugins {
  id("org.jetbrains.kotlin.jvm")
}

apolloLibrary(
  namespace = "com.apollographql.apollo.cache.http"
)

dependencies {
  api(libs.okhttp)
  api(project(":apollo-api"))
  api(project(":apollo-runtime"))
  implementation(libs.moshi)
  implementation(libs.kotlinx.datetime)

  testImplementation(libs.apollo.mockserver)
  testImplementation(libs.kotlin.test.junit)
  testImplementation(libs.truth)
}
