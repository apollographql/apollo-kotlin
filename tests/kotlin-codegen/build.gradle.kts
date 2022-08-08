plugins {
  kotlin("jvm")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation(libs.apollo.api)
  testImplementation(libs.kotlin.test.junit)
}

apollo {
  packageName.set("variables")
}
