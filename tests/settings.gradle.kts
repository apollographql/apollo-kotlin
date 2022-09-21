rootProject.name = "apollo-tests"

// Include all tests
rootProject.projectDir
    .listFiles()!!
    .filter { it.isDirectory}
    .flatMap {
      it.walk()
    }
    .filter { it.isDirectory && File(it, "build.gradle.kts").exists() }
    .forEach {
      val project = it.relativeTo(rootProject.projectDir).path.replace(File.separatorChar, ':')
      include(project)
    }

includeBuild("../")

pluginManagement {
  includeBuild("../build-logic")

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
