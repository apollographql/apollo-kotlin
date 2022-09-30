plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.library")
}

apolloLibrary {
  javaModuleName("com.apollographql.apollo3.runtime.java")
}

repositories {
  mavenCentral()
}
dependencies {
  api(project(":libraries:apollo-api-java"))
}
