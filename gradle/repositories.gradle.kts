listOf(pluginManagement.repositories, dependencyResolutionManagement.repositories).forEach {
  it.apply {
    // Uncomment this one to use the Kotlin "dev" repository
    // maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev/") }
    mavenCentral()

    exclusiveContent {
      // Uncomment to use a "dev" version of Compose when bumping Kotlin versions
      forRepository {
        // Androidx "dev" repository for Compose
        maven { url = uri("https://androidx.dev/storage/compose-compiler/repository/") }
      }
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
        includeModule("org.jetbrains.kotlinx", "kotlinx-benchmark-plugin")
        includeModule("com.gradle.publish", "plugin-publish-plugin")
        includeModule("com.github.ben-manes", "gradle-versions-plugin")
        includeModule("com.gradle", "gradle-enterprise-gradle-plugin")

        // For org.jetbrains.intellij.platform
        includeModule("gradle.plugin.org.jetbrains.gradle.plugin.idea-ext", "gradle-idea-ext")

        // For org.jetbrains.changelog
        includeModule("org.jetbrains.changelog", "org.jetbrains.changelog.gradle.plugin")
        includeModule("org.jetbrains.intellij.plugins", "gradle-changelog-plugin")
      }
    }

    exclusiveContent {
      // TODO Currently, org.jetbrains.intellij.platform is only available in the snapshots repository.
      // It will be available at the gradlePluginPortal when stable.
      forRepository {
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
      }
      filter {
        // For org.jetbrains.intellij.platform
        includeModule("org.jetbrains.intellij.platform", "intellij-platform-gradle-plugin")
      }
    }
  }
}
