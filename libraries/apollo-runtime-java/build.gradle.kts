plugins {
  id("org.jetbrains.kotlin.jvm")
}

apolloLibrary(
  namespace = "com.apollographql.apollo.runtime.java",
)

dependencies {
  api(project(":apollo-api-java"))
}
