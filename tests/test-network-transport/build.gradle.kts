plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation("com.apollographql.apollo3:apollo-runtime")
  implementation("com.apollographql.apollo3:apollo-mockserver")
  testImplementation(libs.kotlin.test)
  testImplementation(libs.junit)
  testImplementation(libs.turbine)
  testImplementation("com.apollographql.apollo3:apollo-testing-support")
}

apollo {
  packageName.set("testnetworktransport")
  generateTestBuilders.set(true)
}
