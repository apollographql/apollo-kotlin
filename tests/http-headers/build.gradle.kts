plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo")
}

apolloTest()

dependencies {
  implementation(libs.apollo.runtime)
  testImplementation(libs.apollo.testingsupport)
  testImplementation(libs.kotlin.test.junit)
  testImplementation(libs.apollo.mockserver)
}

apollo {
  service("service") {
    packageName.set("httpheaders")
    mapScalarToUpload("Upload")
  }
}
