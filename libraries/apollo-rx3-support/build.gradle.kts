/*
 * This file is auto generated from apollo-rx2-support by rxjava3.main.kts, do not edit manually.
 */
plugins {
  id("org.jetbrains.kotlin.jvm")
}

apolloLibrary (
  javaModuleName = "com.apollographql.apollo3.rx3"
)

dependencies {
  implementation(project(":apollo-api"))
  api(libs.rx.java3)
  api(libs.kotlinx.coroutines.rx3)

  api(project(":apollo-runtime"))
  api(project(":apollo-normalized-cache"))
}
