plugins {
  id(libs.plugins.apollo.get().toString())
  id(libs.plugins.kotlin.jvm.get().toString())
}

dependencies {
  implementation(libs.apollo.api)
  testImplementation(libs.kotlin.test.junit)
}

apollo {
  packageName.set("variables")
}
