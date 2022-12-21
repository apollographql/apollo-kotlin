/*
 * This file is auto generated from apollo-rx2-support-java by rxjava3.main.kts, do not edit manually.
 */
plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.library")
}

apolloLibrary {
  javaModuleName("com.apollographql.apollo3.rx3.java")
}

dependencies {
  api(golatac.lib("rx.java3"))
  api(project(":apollo-runtime-java"))
}
