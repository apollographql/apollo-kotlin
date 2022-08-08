plugins {
  id(libs.plugins.apollo.get().toString())
  id(libs.plugins.kotlin.jvm.get().toString())
}

dependencies {
  implementation(libs.apollo.runtime)
  implementation(libs.apollo.normalizedCache)
  implementation(libs.apollo.mockserver)
  implementation(libs.apollo.rx2)
  testImplementation(libs.kotlin.test.junit)
}

apollo {
  packageName.set("rxjava")
}
