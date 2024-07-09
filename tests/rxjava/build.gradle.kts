plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo")
}

apolloTest()

dependencies {
  implementation(libs.apollo.runtime)
  implementation(libs.apollo.normalizedcache)
  implementation(libs.apollo.mockserver)
  implementation(libs.kotlinx.coroutines.rx2)
  testImplementation(libs.kotlin.test.junit)
}

apollo {
  service("service") {
    packageName.set("rxjava")
  }
}
