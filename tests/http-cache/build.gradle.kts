plugins {
  id("com.apollographql.apollo3")
  id("org.jetbrains.kotlin.jvm")
}

dependencies {
  implementation("com.apollographql.apollo3:apollo-runtime")
  implementation("com.apollographql.apollo3:apollo-http-cache")
  implementation("com.apollographql.apollo3:apollo-mockserver")
  testImplementation(kotlin("test-junit"))
  testImplementation("com.apollographql.apollo3:apollo-testing-support")
}

apollo {
  packageName.set("httpcache")
}