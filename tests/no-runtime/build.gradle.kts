plugins {
  kotlin("jvm")
  id("com.apollographql.apollo3")
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
