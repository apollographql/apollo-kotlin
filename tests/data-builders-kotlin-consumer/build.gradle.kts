plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo")
}

apolloTest()

dependencies {
  implementation("com.apollographql.apollo:apollo-normalized-cache")
  testImplementation(kotlin("test-junit"))
  testImplementation(testFixtures(project(":data-builders-kotlin")))
}


