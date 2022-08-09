plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo3")
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
