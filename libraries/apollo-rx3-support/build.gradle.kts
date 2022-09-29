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
  implementation(project(":libraries:apollo-api"))
  api(golatac.lib("rx.java3"))
  api(golatac.lib("kotlinx.coroutines.rx3"))

  api(project(":libraries:apollo-runtime"))
  api(project(":libraries:apollo-normalized-cache"))
}
