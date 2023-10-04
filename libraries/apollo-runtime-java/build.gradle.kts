plugins {
  id("org.jetbrains.kotlin.jvm")
}

apolloLibrary(
  javaModuleName = "com.apollographql.apollo3.runtime.java",
)

dependencies {
  api(project(":apollo-api-java"))
}
