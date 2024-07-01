plugins {
  id("org.jetbrains.kotlin.jvm")
}

apolloLibrary(
    namespace = "com.apollographql.apollo.api.java"
)

dependencies {
  api(project(":apollo-api"))
  api(libs.okhttp)
  compileOnly(libs.guava.jre)
}
