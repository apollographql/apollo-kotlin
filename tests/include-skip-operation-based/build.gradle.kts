plugins {
  id("com.apollographql.apollo3")
  id("org.jetbrains.kotlin.jvm")
}

dependencies {
  implementation(libs.apollo.runtime)
  implementation(libs.apollo.httpCache)
  implementation(libs.apollo.mockserver)
  testImplementation(libs.kotlin.test.junit)
  testImplementation(libs.apollo.testingSupport)
}

apollo {
  packageName.set("com.example")
  generateTestBuilders.set(true)
}
