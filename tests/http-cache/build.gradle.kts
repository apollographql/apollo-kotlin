plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation(libs.apollo.runtime)
  implementation(libs.apollo.httpCache)
  implementation(libs.apollo.mockserver)
  testImplementation(libs.kotlin.test.junit)
  testImplementation(libs.apollo.testingsupport)
}

apollo {
  packageName.set("httpcache")
  generateDataBuilders.set(true)
}
