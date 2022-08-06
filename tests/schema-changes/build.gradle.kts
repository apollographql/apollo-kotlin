plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation(libs.apollo.runtime)
  implementation(libs.apollo.normalizedCache)
  testImplementation(libs.apollo.testingSupport)
  testImplementation(libs.kotlin.test)
  testImplementation(libs.turbine)
}

apollo {
  packageName.set("schema.changes")
  codegenModels.set("responseBased")
}
