plugins {
  id("org.jetbrains.kotlin.jvm")
}

apolloLibrary(
    namespace = "com.apollographql.apollo.cache.http",
    description = "Apollo GraphQL http cache network transport",
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
