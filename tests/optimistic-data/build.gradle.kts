plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo")
}

apolloTest()

dependencies {
  implementation(libs.apollo.runtime)
  implementation(libs.apollo.normalizedcache)
  testImplementation(libs.apollo.testingsupport.internal)
  testImplementation(libs.apollo.mockserver)
  testImplementation(libs.turbine)
}

apollo {
  service("service") {
    packageName.set("optimistic")
  }
}
