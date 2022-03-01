plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo3")
}

dependencies {
  apolloMetadata(project(":module1"))
}

// the apollo configuration will be added here by the test code