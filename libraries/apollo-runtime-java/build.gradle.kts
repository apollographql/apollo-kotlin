plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.library")
}

apolloLibrary {
  javaModuleName("com.apollographql.apollo3.runtime.java")
}

dependencies {
  api(project(":apollo-api-java"))
}
