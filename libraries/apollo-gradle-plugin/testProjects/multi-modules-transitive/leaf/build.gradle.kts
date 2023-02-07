plugins {
  id("org.jetbrains.kotlin.jvm")
  alias(libs.plugins.apollo)
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
    packageNamesFromFilePaths()
    dependsOn(project(":node"))
  }
}
