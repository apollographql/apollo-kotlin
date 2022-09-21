rootProject.name = "apollo-kotlin"

rootProject.projectDir
    .listFiles()!!
    .filter { it.isDirectory }
    .filter { it.name.startsWith("apollo-") }
    .filter { File(it, "build.gradle.kts").exists() }
    .forEach {
      include(it.name)
    }

pluginManagement {
  includeBuild("build-logic")

  repositories {
    mavenCentral()
    google()
    gradlePluginPortal {
      content {
        includeModule("me.champeau.gradle", "japicmp-gradle-plugin")
        includeModule("com.gradle.publish", "plugin-publish-plugin")
        includeModule("com.github.ben-manes", "gradle-versions-plugin")
      }
    }
    mavenLocal()
  }
}

