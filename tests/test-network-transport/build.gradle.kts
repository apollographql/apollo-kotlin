plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo3")
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
