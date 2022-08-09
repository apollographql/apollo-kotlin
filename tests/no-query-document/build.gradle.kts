plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation(libs.apollo.runtime)
  implementation(libs.apollo.mockserver)
  implementation(libs.apollo.testingsupport)
  testImplementation(libs.kotlin.test)
  testImplementation(libs.junit)
}

apollo {
  packageName.set("reserved")
  generateQueryDocument.set(false)
}
