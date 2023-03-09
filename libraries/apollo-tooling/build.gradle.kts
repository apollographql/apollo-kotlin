plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.library")
}

dependencies {
  api(project(":apollo-compiler"))

  implementation(project(":apollo-annotations"))
  implementation(project(":apollo-ast"))
  implementation(golatac.lib("okhttp"))
  implementation(golatac.lib("kotlinx.serialization.json"))

  testImplementation(golatac.lib("junit"))
  testImplementation(golatac.lib("truth"))
}
