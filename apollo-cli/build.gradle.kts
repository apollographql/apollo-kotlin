plugins {
  id("apollo.library.jvm")
  id("application")
}

dependencies {
  implementation(projects.apolloTooling)
  implementation(projects.apolloAnnotations)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.clikt)
}

application {
  mainClass.set("com.apollographql.apollo3.cli.MainKt")
}
