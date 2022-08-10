plugins {
  id("apollo.test.jvm")
}

dependencies {
  implementation(libs.apollo.api)
  testImplementation(libs.kotlin.test.junit)
}

apollo {
  packageName.set("variables")
}
