plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.library")
}

apolloLibrary {
  javaModuleName("com.apollographql.apollo3.cache.http")
}

dependencies {
  api(golatac.lib("okhttp"))
  api(project(":apollo-api"))
  api(project(":apollo-runtime"))
  implementation(golatac.lib("moshi"))
  implementation(golatac.lib("kotlinx.datetime"))

  testImplementation(project(":apollo-mockserver"))
  testImplementation(golatac.lib("kotlin.test.junit"))
  testImplementation(golatac.lib("truth"))
}
