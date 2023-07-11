plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.library")
}

apolloLibrary {
  javaModuleName("com.apollographql.apollo3.rx2.java")
}

dependencies {
  api(libs.rx.java2)
  api(project(":apollo-runtime-java"))
}
