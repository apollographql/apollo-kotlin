plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo3")
}

apolloTest()

dependencies {
  implementation(libs.apollo.runtime)
  implementation(libs.apollo.mockserver)
  testImplementation(libs.kotlin.test.junit)
  testImplementation(libs.apollo.testingsupport)
}

apollo {
  service("service") {
    packageName.set("httpheaders")
    mapScalarToUpload("Upload")
  }
}
