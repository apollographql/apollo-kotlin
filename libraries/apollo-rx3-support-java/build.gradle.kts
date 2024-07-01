/*
 * This file is auto generated from apollo-rx2-support-java by rxjava3.main.kts, do not edit manually.
 */
plugins {
  id("org.jetbrains.kotlin.jvm")
}

apolloLibrary(
  namespace  = "com.apollographql.apollo.rx3.java"
)

dependencies {
  api(libs.rx.java3)
  api(project(":apollo-runtime-java"))
}
