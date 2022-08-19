plugins {
  id("java")
  id("apollo.test")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation(libs.apollo.java)
  implementation(libs.apollo.mockserver)
  testImplementation(libs.junit)
  testImplementation(libs.truth)
}

apollo {
  packageName.set("test")
  generateFragmentImplementations.set(true)
}
