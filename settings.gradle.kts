rootProject.name = "apollo-android"

rootProject.projectDir
    .listFiles()!!
    .filter { it.isDirectory }
    .filter { it.name.startsWith("apollo-") }
    .filter { File(it, "build.gradle.kts").exists() }
    .forEach {
      include(it.name)
    }

includeBuild("build-logic")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
  repositories {
    mavenCentral()
    google()
  }
}

