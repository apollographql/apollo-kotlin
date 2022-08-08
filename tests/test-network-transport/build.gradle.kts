plugins {
  id(libs.plugins.kotlin.jvm.get().toString())
  id(libs.plugins.apollo.get().toString())
}

dependencies {
  implementation(libs.apollo.runtime)
  implementation(libs.apollo.mockserver)
  testImplementation(libs.kotlin.test)
  testImplementation(libs.junit)
  testImplementation(libs.turbine)
  testImplementation(libs.apollo.testingSupport)
}

apollo {
  packageName.set("testnetworktransport")
  generateTestBuilders.set(true)
}
