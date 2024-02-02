plugins {
  id("org.jetbrains.kotlin.jvm")
}

apolloTest()

dependencies {
  implementation(libs.apollo.compiler)
}