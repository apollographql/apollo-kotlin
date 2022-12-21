plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.library")
}

apolloLibrary {
  javaModuleName("com.apollographql.apollo3.api.java")
}

dependencies {
  api(project(":apollo-api"))
  api(golatac.lib("okhttp"))
  compileOnly(golatac.lib("guava.jre"))
}
