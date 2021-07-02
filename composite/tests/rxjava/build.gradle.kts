plugins {
  id("com.apollographql.apollo3")
  id("org.jetbrains.kotlin.jvm")
}

dependencies {
  implementation("com.apollographql.apollo3:apollo-runtime")
  implementation("com.apollographql.apollo3:apollo-normalized-cache")
  implementation("com.apollographql.apollo3:apollo-mockserver")
  implementation("com.apollographql.apollo3:apollo-rx2-support")
  testImplementation(kotlin("test-junit"))
}

apollo {
  packageName.set("rxjava")
}