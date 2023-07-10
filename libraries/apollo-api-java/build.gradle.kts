plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.library")
}

apolloLibrary {
  javaModuleName("com.apollographql.apollo3.api.java")
}

dependencies {
  api(project(":apollo-api"))
  api(libs.okhttp)
  compileOnly(libs.guava.jre)
}
