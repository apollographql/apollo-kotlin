plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo")
}

apolloTest()

dependencies {
  implementation(libs.apollo.api)
  testImplementation(libs.apollo.runtime)
  testImplementation(libs.kotlin.test)
  testImplementation(libs.apollo.mockserver)
}

apollo {
  service("service") {
    packageName.set("com.example")
  }
}
