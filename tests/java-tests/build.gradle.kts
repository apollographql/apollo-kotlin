plugins {
  id("java")
  id("com.apollographql.apollo3")
}

apolloTest()

dependencies {
  implementation(libs.apollo.runtime)
  implementation(libs.apollo.httpCache)
  implementation(libs.apollo.normalizedcache)
  implementation(libs.apollo.normalizedcache.sqlite)
  implementation(libs.apollo.mockserver)
  implementation(libs.apollo.rx2)
  testImplementation(libs.junit)
  testImplementation(libs.truth)
}

apollo {
  service("service") {
    packageName.set("javatest")
    generateModelBuilders.set(true)
    generateDataBuilders.set(true)
    mapScalarToJavaString("LanguageCode")
    mapScalarToJavaObject("Json")
    mapScalarToJavaLong("Long")
  }
}
