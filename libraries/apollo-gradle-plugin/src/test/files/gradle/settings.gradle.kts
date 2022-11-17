rootProject.name="testProject"

apply(from = "../../../../gradle/test.settings.gradle.kts")
pluginManagement {
  repositories {
    maven {
      url = uri("../../../../build/localMaven")
    }
    gradlePluginPortal()
  }
}
