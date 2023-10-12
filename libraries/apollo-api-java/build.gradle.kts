plugins {
  id("org.jetbrains.kotlin.jvm")
}

apolloLibrary(
    javaModuleName = "com.apollographql.apollo3.api.java"
)

dependencies {
  api(project(":apollo-api"))
  api(libs.okhttp)
  compileOnly(libs.guava.jre)
}
