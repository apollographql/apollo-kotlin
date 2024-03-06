plugins {
  id("org.jetbrains.kotlin.jvm")
  id("application")
}

apolloLibrary(
    namespace = "com.apollographql.apollo3.cli",
)

dependencies {
  implementation(project(":apollo-tooling"))
  implementation(project(":apollo-annotations"))
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.clikt)
}

application {
  mainClass.set("com.apollographql.apollo3.cli.MainKt")
}
