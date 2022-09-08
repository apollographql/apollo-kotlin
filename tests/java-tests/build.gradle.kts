plugins {
  id("java")
  id("apollo.test")
  id("com.apollographql.apollo3")
}

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
  packageName.set("javatest")
  generateModelBuilder.set(true)
  mapScalarToJavaString("LanguageCode")
  mapScalarToJavaObject("Json")
  mapScalarToJavaLong("Long")
}
