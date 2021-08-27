plugins {
  id("com.apollographql.apollo3")
  id("org.jetbrains.kotlin.jvm")
}

dependencies {
  implementation("com.apollographql.apollo3:apollo-api")
  testImplementation(kotlin("test-junit"))
}

apollo {
  packageName.set("variables")
}