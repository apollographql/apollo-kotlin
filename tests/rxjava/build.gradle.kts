plugins {
  id("com.apollographql.apollo3")
  id("org.jetbrains.kotlin.jvm")
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
