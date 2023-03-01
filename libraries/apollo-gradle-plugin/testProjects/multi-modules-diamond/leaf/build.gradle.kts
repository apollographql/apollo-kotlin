plugins {
  id("org.jetbrains.kotlin.jvm")
  alias(libs.plugins.apollo)
  id("application")
}

dependencies {
  implementation(kotlin("stdlib"))
  testImplementation(libs.kotlin.test.junit)
  implementation(libs.apollo.api)

  implementation(project(":node1"))
  implementation(project(":node2"))
}

application {
  mainClass.set("LeafKt")
}

apollo {
  service("service") {
    packageNamesFromFilePaths()
    dependsOn(project(":node1"))
    dependsOn(project(":node2"))
  }
}
