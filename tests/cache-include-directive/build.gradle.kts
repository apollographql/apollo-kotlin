plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.test")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation(libs.apollo.runtime)
  implementation(libs.apollo.normalizedcache)
  testImplementation(libs.kotlin.test)
  testImplementation(libs.okhttp)
  testImplementation(project(mapOf("path" to ":input")))
}

apollo {
  service("service") {
    packageName.set("com.example")
  }
}
