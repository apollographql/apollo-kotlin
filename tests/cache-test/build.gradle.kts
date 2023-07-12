plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.test")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation(libs.apollo.runtime)
  implementation(libs.apollo.normalizedcache)
  implementation(libs.apollo.testingsupport)
  testImplementation(libs.kotlin.test.junit)
}

apollo {
  generateSourcesDuringGradleSync.set(false)
  service("test") {
    addTypename.set("always")
    packageName.set("test")
    generateCompiledField.set(false)
  }
}
