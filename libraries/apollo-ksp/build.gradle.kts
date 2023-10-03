plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.library")
}

apolloLibrary {
  javaModuleName("com.apollographql.apollo3.ksp")
}

dependencies {
  implementation(project(":apollo-compiler"))
  implementation(libs.ksp.api)
  implementation(project(":apollo-ast"))
  testImplementation(libs.kotlin.test)
}
