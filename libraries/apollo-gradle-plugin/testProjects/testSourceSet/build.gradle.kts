
plugins {
  alias(libs.plugins.apollo)
  alias(libs.plugins.kotlin.jvm)
}

dependencies {
  add("testImplementation", libs.apollo.api)
}

apollo {
  packageNamesFromFilePaths()
  outputDirConnection {
    connectToKotlinSourceSet("test")
  }
}
