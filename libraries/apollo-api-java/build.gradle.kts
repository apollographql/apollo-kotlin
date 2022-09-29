plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.library")
}

apolloLibrary {
  javaModuleName("com.apollographql.apollo3.api.java")
}

dependencies {
  api(project(":libraries:apollo-api"))
  compileOnly(golatac.lib("guava.jre"))
}
