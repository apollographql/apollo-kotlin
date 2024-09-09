plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo")
}

apolloTest()

dependencies {
  implementation(libs.apollo.api)
  implementation(libs.apollo.testingsupport)
  testImplementation(libs.kotlin.test)
  testImplementation(libs.apollo.testingsupport)
  testImplementation(libs.apollo.normalizedcache)
}

apollo {
  service("service") {
    packageName.set("com.example")
    mapScalar("Number", "kotlin.String")
  }
}
