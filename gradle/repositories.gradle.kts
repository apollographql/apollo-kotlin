listOf(pluginManagement.repositories, dependencyResolutionManagement.repositories).forEach {
  it.apply {
    mavenCentral()

    exclusiveContent {
      forRepository(::google)
      filter {
        includeModuleByRegex("com\\.android.*", ".*")
        includeModuleByRegex("androidx\\..*", ".*")
        includeModuleByRegex("com.google.testing.platform", ".*")
      }
    }

    exclusiveContent {
      forRepository(::gradlePluginPortal)
      filter {
        includeModule("org.gradle.kotlin.embedded-kotlin", "org.gradle.kotlin.embedded-kotlin.gradle.plugin")
        includeModule("org.gradle.kotlin", "gradle-kotlin-dsl-plugins")
        includeModule("me.champeau.gradle", "japicmp-gradle-plugin")
        includeModule("com.gradle.publish", "plugin-publish-plugin")
        includeModule("com.github.ben-manes", "gradle-versions-plugin")
        // Because we use 1.6.10 during sync and this version is not on mavenCentral
        includeVersion("org.jetbrains.kotlin.plugin.serialization", "org.jetbrains.kotlin.plugin.serialization.gradle.plugin", "1.6.10")

        // For org.jetbrains.intellij
        includeModule("org.jetbrains.intellij", "org.jetbrains.intellij.gradle.plugin")
        includeModule("org.jetbrains.intellij.plugins", "gradle-intellij-plugin")
        includeModule("gradle.plugin.org.jetbrains.gradle.plugin.idea-ext", "gradle-idea-ext")

        // For org.jetbrains.changelog
        includeModule("org.jetbrains.changelog", "org.jetbrains.changelog.gradle.plugin")
        includeModule("org.jetbrains.intellij.plugins", "gradle-changelog-plugin")
      }
    }

    exclusiveContent {
      @Suppress("DEPRECATION")
      forRepository(::jcenter)
      filter {
        // https://github.com/Kotlin/kotlinx-nodejs/issues/16
        includeModule("org.jetbrains.kotlinx", "kotlinx-nodejs")
      }
    }
  }
}
