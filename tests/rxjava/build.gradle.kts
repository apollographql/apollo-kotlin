plugins {
  id("apollo.test.jvm")
}

dependencies {
  implementation(libs.apollo.runtime)
  implementation(libs.apollo.normalizedcache)
  implementation(libs.apollo.mockserver)
  implementation(libs.apollo.rx2)
  testImplementation(libs.kotlin.test.junit)
}

apollo {
  packageName.set("rxjava")
}
