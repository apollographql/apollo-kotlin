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
  implementation(projects.apolloApi)
  api(libs.rx.java3)
  api(libs.kotlinx.coroutines.rx3)

  api(projects.apolloRuntime)
  api(projects.apolloNormalizedCache)
}
