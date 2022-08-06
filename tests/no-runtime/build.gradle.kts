plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation("com.apollographql.apollo3:apollo-api")
  implementation(projects.sampleServer)
  implementation("com.apollographql.apollo3:apollo-testing-support")
  testImplementation(libs.kotlin.test)
  testImplementation(libs.junit)
  testImplementation(libs.okHttp)
}

apollo {
  packageName.set("com.example")
}
