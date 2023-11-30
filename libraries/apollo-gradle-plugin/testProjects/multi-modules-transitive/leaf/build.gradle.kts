plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo3")
  id("application")
}

dependencies {
  implementation(libs.apollo.api)

  implementation(kotlin("stdlib"))
  testImplementation(libs.kotlin.test.junit)

  implementation(project(":node"))
}

application {
  mainClass.set("LeafKt")
}

apollo {
  service("service") {
    dependsOn(project(":node"))
    packageNamesFromFilePaths()
  }
}
