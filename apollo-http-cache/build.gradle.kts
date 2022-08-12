plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.library")
}

apolloLibrary {
  javaModuleName("com.apollographql.apollo3.cache.http")
}

dependencies {
  api(libs.okhttp)
  api(projects.apolloApi)
  api(projects.apolloRuntime)
  implementation(libs.moshi)
  implementation(libs.kotlinx.datetime)

  testImplementation(projects.apolloMockserver)
  testImplementation(libs.kotlin.test.junit)
  testImplementation(libs.truth)
}
