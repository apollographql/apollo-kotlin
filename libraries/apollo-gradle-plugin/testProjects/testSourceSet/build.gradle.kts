
plugins {
  alias(libs.plugins.apollo)
  alias(libs.plugins.kotlin.jvm)
}

dependencies {
  add("testImplementation", libs.apollo.api)
}

apollo {
  service("service") {
    packageNamesFromFilePaths()
    outputDirConnection {
      connectToKotlinSourceSet("test")
    }
  }
}
