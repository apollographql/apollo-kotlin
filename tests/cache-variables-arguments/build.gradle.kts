plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo3")
}

apolloTest()

dependencies {
  implementation(libs.apollo.runtime)
  implementation(libs.apollo.normalizedcache)
  testImplementation(libs.kotlin.test)
}

apollo {
  service("service") {
    packageName.set("cache.include")
  }
}
