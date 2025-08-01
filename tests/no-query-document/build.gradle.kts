plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo")
}

apolloTest()

dependencies {
  implementation(libs.apollo.runtime)
  implementation(libs.apollo.testingsupport.internal)
  implementation(libs.apollo.mockserver)
  testImplementation(libs.junit)
}

apollo {
  service("service") {
    packageName.set("reserved")
    generateQueryDocument.set(false)
  }
}
