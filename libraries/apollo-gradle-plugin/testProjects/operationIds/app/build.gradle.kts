plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.apollo)
}

apollo {
  service("service") {

    packageNamesFromFilePaths()
    plugin(project(":apollo-compiler-plugin"))
  }
}