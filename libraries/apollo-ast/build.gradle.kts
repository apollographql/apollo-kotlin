plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.library")
  id("org.jetbrains.kotlin.plugin.serialization")
}

apolloLibrary {
  javaModuleName("com.apollographql.apollo3.ast")
}

dependencies {
  api(okio())
  api(project(":apollo-annotations"))

  implementation(golatac.lib("kotlinx.serialization.json"))

  testImplementation(golatac.lib("kotlin.test.junit"))
}
