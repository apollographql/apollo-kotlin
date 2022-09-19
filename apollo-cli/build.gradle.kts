plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.library")
  id("application")
}

dependencies {
  implementation(project(":apollo-tooling"))
  implementation(project(":apollo-annotations"))
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.clikt)
}

application {
  mainClass.set("com.apollographql.apollo3.cli.MainKt")
}
