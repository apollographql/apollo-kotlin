plugins {
  id("org.jetbrains.kotlin.jvm")
}

apolloLibrary(
  namespace = "com.apollographql.apollo.rx2.java"
)

dependencies {
  api(libs.rx.java2)
  api(project(":apollo-runtime-java"))
}
