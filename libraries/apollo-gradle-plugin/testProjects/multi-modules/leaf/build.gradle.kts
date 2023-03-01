plugins {
  id("org.jetbrains.kotlin.jvm")
  alias(libs.plugins.apollo)
  id("application")
}

dependencies {
  implementation(kotlin("stdlib"))
  implementation(libs.apollo.api)
  testImplementation(libs.kotlin.test.junit)

  implementation(project(":root"))
}

application {
  mainClass.set("LeafKt")
}

apollo {
  service("service") {
    packageNamesFromFilePaths()
    dependsOn(project(":root"))
  }
}
