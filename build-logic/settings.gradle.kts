rootProject.name = "build-logic"

apply(from = "../gradle/dependencies.gradle")

pluginManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}
