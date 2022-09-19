plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.library")
}

dependencies {
  implementation(project(":apollo-annotations"))
  implementation(project(":apollo-ast"))
  api(project(":apollo-compiler"))
  implementation(libs.moshi)
  implementation(libs.moshix.sealed.runtime)
  implementation(libs.okhttp)

  implementation(libs.moshi)
  testImplementation(libs.junit)
  testImplementation(libs.truth)
}
