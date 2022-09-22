plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.library")
}

dependencies {
  implementation(project(":apollo-annotations"))
  implementation(project(":apollo-ast"))
  api(project(":apollo-compiler"))
  implementation(golatac.lib("moshi"))
  implementation(golatac.lib("moshix.sealed.runtime"))
  implementation(golatac.lib("okhttp"))

  implementation(golatac.lib("moshi"))
  testImplementation(golatac.lib("junit"))
  testImplementation(golatac.lib("truth"))
}
