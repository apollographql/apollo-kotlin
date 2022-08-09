/*
 * This file is auto generated from apollo-rx2-support by rxjava3.main.kts, do not edit manually.
 */
plugins {
  id("apollo.library.jvm")
}

dependencies {
  implementation(projects.apolloApi)
  api(libs.rx.java3)
  api(libs.kotlinx.coroutines.rx3)

  api(projects.apolloRuntime)
  api(projects.apolloNormalizedCache)
}

apolloConvention {
  javaModuleName.set("com.apollographql.apollo3.rx3")
}
