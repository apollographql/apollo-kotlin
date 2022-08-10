plugins {
  id("apollo.test.jvm")
}

dependencies {
  implementation(libs.apollo.runtime)
  implementation(libs.apollo.mockserver)
  testImplementation(libs.kotlin.test)
  testImplementation(libs.junit)
  testImplementation(libs.turbine)
  testImplementation(libs.apollo.testingsupport)
}

apollo {
  packageName.set("testnetworktransport")
  generateTestBuilders.set(true)
}
