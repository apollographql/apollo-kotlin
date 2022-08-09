plugins {
  id("apollo.library.jvm")
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

apolloConvention {
  javaModuleName.set("com.apollographql.apollo3.cache.http")
}
