plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo")
}

apolloTest()

dependencies {
  implementation(libs.apollo.runtime)
  implementation(libs.apollo.testingsupport.internal)
  testImplementation(libs.junit)
  testImplementation(libs.okhttp)
  testImplementation(libs.apollo.mockserver)
}

apollo {
  service("service") {
    packageName.set("com.example")
  }
}
