plugins {
  id("apollo.test.jvm")
}

dependencies {
  implementation(libs.apollo.api)
  implementation(projects.sampleServer)
  implementation(libs.apollo.testingsupport)
  testImplementation(libs.kotlin.test)
  testImplementation(libs.junit)
  testImplementation(libs.okhttp)
}

apollo {
  packageName.set("com.example")
}
