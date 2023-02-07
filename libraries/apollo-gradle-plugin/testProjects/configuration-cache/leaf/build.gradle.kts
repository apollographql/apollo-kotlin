plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.apollo)
}

dependencies {
  implementation(kotlin("stdlib"))
  implementation(libs.apollo.api)

  implementation(project(":root"))
}

apollo {
  service("service") {
    packageName.set("com.example.leaf")
    dependsOn(project(":root"))
  }
}
