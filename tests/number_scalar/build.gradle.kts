plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo")
}

apolloTest()

dependencies {
  implementation(libs.apollo.api)
  testImplementation(libs.apollo.testingsupport)
  testImplementation(libs.apollo.normalizedcache)
}

apollo {
  service("service") {
    packageName.set("com.example")
    mapScalar("Number", "kotlin.String")
  }
}
