plugins {
  kotlin("jvm")
  id("com.apollographql.apollo3")
  id("application")
}

dependencies {
  implementation(kotlin("stdlib"))
  implementation(libs.apollo.api)
  testImplementation(libs.kotlin.test.junit)

  implementation(project(":root"))
  apolloMetadata(project(":root"))
}

application {
  mainClass.set("LeafKt")
}

apollo {
  packageName.set("com.library")
}
