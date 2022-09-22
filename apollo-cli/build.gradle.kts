plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.library")
  id("application")
}

dependencies {
  implementation(project(":apollo-tooling"))
  implementation(project(":apollo-annotations"))
  implementation(golatac.lib("kotlinx.serialization.json"))
  implementation(golatac.lib("clikt"))
}

application {
  mainClass.set("com.apollographql.apollo3.cli.MainKt")
}
