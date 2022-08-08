plugins {
  id(libs.plugins.apollo.get().toString())
  id(libs.plugins.kotlin.jvm.get().toString())
}

dependencies {
  implementation(libs.apollo.runtime)
  implementation(libs.apollo.httpCache)
  implementation(libs.apollo.mockserver)
  testImplementation(libs.kotlin.test.junit)
  testImplementation(libs.apollo.testingSupport)
}

apollo {
  packageName.set("httpcache")
}
