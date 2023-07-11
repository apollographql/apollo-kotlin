plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.test")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation(libs.apollo.runtime)
  implementation(libs.apollo.httpCache)
  implementation(libs.apollo.normalizedcache)
  implementation(libs.apollo.mockserver)
  testImplementation(libs.kotlin.test.junit)
  testImplementation(libs.apollo.testingsupport)
}

apollo {
  service("service") {
    packageName.set("com.example")
    generateDataBuilders.set(true)
  }
}
