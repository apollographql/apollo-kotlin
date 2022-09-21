pluginManagement {
  repositories {
    mavenCentral()
    mavenLocal()
    gradlePluginPortal {
      content {
        includeModule("org.gradle.kotlin.embedded-kotlin", "org.gradle.kotlin.embedded-kotlin.gradle.plugin")
        includeGroup("org.gradle.kotlin")
      }
    }
  }
}