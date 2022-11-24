listOf(pluginManagement.repositories, dependencyResolutionManagement.repositories).forEach {
  it.apply {
    mavenCentral()
    google()
    gradlePluginPortal {
      content {
        includeModule("org.gradle.kotlin.embedded-kotlin", "org.gradle.kotlin.embedded-kotlin.gradle.plugin")
        includeGroup("org.gradle.kotlin")
        includeModule("me.champeau.gradle", "japicmp-gradle-plugin")
        includeModule("com.gradle.publish", "plugin-publish-plugin")
        includeModule("com.github.ben-manes", "gradle-versions-plugin")
      }
    }
    @Suppress("DEPRECATION")
    jcenter {
      content {
        // https://github.com/Kotlin/kotlinx-nodejs/issues/16
        includeModule("org.jetbrains.kotlinx", "kotlinx-nodejs")
      }
    }
  }
}
