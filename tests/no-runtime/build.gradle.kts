plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo3")
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
