/*
 * This file is auto generated from apollo-rx2-support by rxjava3.main.kts, do not edit manually.
 */
plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.library")
}

apolloLibrary {
  javaModuleName("com.apollographql.apollo3.rx3")
}

dependencies {
  implementation(project(":apollo-api"))
  api(golatac.lib("rx.java3"))
  api(golatac.lib("kotlinx.coroutines.rx3"))

  api(project(":apollo-runtime"))
  api(project(":apollo-normalized-cache"))
}
