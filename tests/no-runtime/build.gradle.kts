plugins {
  id(libs.plugins.kotlin.jvm.get().toString())
  id(libs.plugins.apollo.get().toString())
}

dependencies {
  implementation(libs.apollo.api)
  implementation(projects.sampleServer)
  implementation(libs.apollo.testingSupport)
  testImplementation(libs.kotlin.test)
  testImplementation(libs.junit)
  testImplementation(libs.okHttp)
}

apollo {
  packageName.set("com.example")
}
