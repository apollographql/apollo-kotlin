// the plugin management block is evaluated first separately, do not change this to
// listOf(pluginManagement.repositories, dependencyResolutionManagement.repositories)
// instead that would change the semantics
// See https://www.linen.dev/s/gradle-community/t/5100108/are-the-rules-for-how-gradle-parses-compiles-buildscript-and
pluginManagement {
  listOf(repositories, dependencyResolutionManagement.repositories).forEach {
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
          includeModule("org.jetbrains.kotlin.plugin.serialization", "org.jetbrains.kotlin.plugin.serialization.gradle.plugin")
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
}
