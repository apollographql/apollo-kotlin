plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.apollo)
}

dependencies {
  implementation(kotlin("stdlib"))
  implementation(libs.apollo.api)

  implementation(project(":root"))
  apolloMetadata(project(":root"))
}

apollo {
  packageName.set("com.example.leaf")
}
