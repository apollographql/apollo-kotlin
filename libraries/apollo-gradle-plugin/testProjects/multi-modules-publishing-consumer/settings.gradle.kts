apply(from = "../../../../gradle/test.settings.gradle.kts")


dependencyResolutionManagement {
  repositories {
    maven {
      name = "pluginTest"
      url = uri("file://${rootDir.parentFile}/localMaven")
    }
  }
}