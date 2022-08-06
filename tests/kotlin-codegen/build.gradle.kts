plugins {
  id("com.apollographql.apollo3")
  id("org.jetbrains.kotlin.jvm")
}

dependencies {
  implementation(libs.apollo.api)
  testImplementation(libs.kotlin.test.junit)
}

apollo {
  packageName.set("variables")
}
