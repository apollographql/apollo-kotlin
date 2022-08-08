plugins {
  id(libs.plugins.kotlin.jvm.get().toString())
  id(libs.plugins.apollo.get().toString())
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
