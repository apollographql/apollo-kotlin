plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.library")
}

apolloLibrary {
  javaModuleName("com.apollographql.apollo3.rx2")
}

dependencies {
  implementation(project(":apollo-api"))
  api(golatac.lib("rx.java2"))
  api(golatac.lib("kotlinx.coroutines.rx2"))

  api(project(":apollo-runtime"))
  api(project(":apollo-normalized-cache"))
}
