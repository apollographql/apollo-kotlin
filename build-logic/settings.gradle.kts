rootProject.name = "build-logic"

dependencyResolutionManagement {
  repositories {
    mavenCentral()
    google()
    gradlePluginPortal {
      content {
        includeModule("me.champeau.gradle", "japicmp-gradle-plugin")
        includeModule("me.tylerbwong.gradle", "metalava-gradle")
        includeModule("com.gradle.publish", "plugin-publish-plugin")
        includeModule("com.github.ben-manes", "gradle-versions-plugin")
      }
    }
  }

  versionCatalogs {
    create("libs") {
      from(files("../gradle/libs.versions.toml"))
    }
  }
}
