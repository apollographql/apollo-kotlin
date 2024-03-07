plugins {
  id("org.jetbrains.kotlin.jvm")
}

apolloLibrary(
  namespace = "com.apollographql.apollo3.runtime.java",
)

dependencies {
  api(project(":apollo-api-java"))
}
