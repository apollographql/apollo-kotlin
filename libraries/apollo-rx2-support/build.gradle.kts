plugins {
  id("org.jetbrains.kotlin.jvm")
}

apolloLibrary(namespace = "com.apollographql.apollo.rx2")

dependencies {
  implementation(project(":apollo-api"))
  api(libs.rx.java2)
  api(libs.kotlinx.coroutines.rx2)

  api(project(":apollo-runtime"))
  api(project(":apollo-normalized-cache"))
}
