plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.library")
}

apolloLibrary {
  javaModuleName("com.apollographql.apollo3.rx2.java")
}

dependencies {
  api(golatac.lib("rx.java2"))
  api(project(":apollo-runtime-java"))
}
