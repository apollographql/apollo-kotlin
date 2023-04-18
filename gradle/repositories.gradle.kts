listOf(pluginManagement.repositories, dependencyResolutionManagement.repositories).forEach {
  it.apply {
    // Uncomment this one to use the Kotlin "dev" repository
    // maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev/") }
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
        includeModule("com.gradle", "gradle-enterprise-gradle-plugin")

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
