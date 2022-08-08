plugins {
  id(libs.plugins.apollo.get().toString())
  java
}

dependencies {
  implementation(libs.apollo.runtime)
  implementation(libs.apollo.httpCache)
  implementation(libs.apollo.normalizedCache)
  implementation(libs.apollo.normalizedCache.sqlite)
  implementation(libs.apollo.mockserver)
  implementation(libs.apollo.rx2)
  testImplementation(libs.junit)
  testImplementation(libs.truth)
}

apollo {
  packageName.set("javatest")
}
