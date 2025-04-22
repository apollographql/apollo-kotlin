listOf(pluginManagement.repositories, dependencyResolutionManagement.repositories).forEach {
  it.apply {
    // Uncomment this one to use the Kotlin "dev" repository
    // maven("https://redirector.kotlinlang.org/maven/dev/")
    // Uncomment this one to use the Sonatype OSSRH snapshots repository
    // maven("https://oss.sonatype.org/content/repositories/snapshots/")
    // Uncomment this one to use the GradleUp repository
    // maven("https://storage.googleapis.com/gradleup/m2")

    mavenCentral()
    exclusiveContent {
      forRepository(::google)
      filter {
        includeGroup("com.android")
        includeGroup("com.android.library")
        includeGroup("com.android.application")
        includeGroup("com.android.databinding")
        includeGroup("com.android.lint")
        includeGroup("com.google.testing.platform")
        /*
         * The com.android.tools groupId is verbose because we don't want to clash with com.android.tools:r8 in the raw repository
         */
        includeModule("com.android.tools", "sdk-common")
        includeModule("com.android.tools", "sdklib")
        includeModule("com.android.tools", "repository")
        includeModule("com.android.tools", "common")
        includeModule("com.android.tools", "dvlib")
        includeModule("com.android.tools", "annotations")
        includeModule("com.android.tools", "play-sdk-proto")
        includeGroupByRegex("com\\.android\\.tools\\..*")
        includeModuleByRegex("androidx\\..*", ".*")
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
        includeModule("com.gradle", "develocity-gradle-plugin")

        // For org.jetbrains.intellij.platform
        includeModule("gradle.plugin.org.jetbrains.gradle.plugin.idea-ext", "gradle-idea-ext")

        // For org.jetbrains.changelog
        includeModule("org.jetbrains.changelog", "org.jetbrains.changelog.gradle.plugin")
        includeModule("org.jetbrains.intellij.plugins", "gradle-changelog-plugin")

        // For org.jetbrains.intellij.platform
        includeModule("org.jetbrains.intellij.platform", "intellij-platform-gradle-plugin")

        // For org.jetbrains.grammarkit
        includeModule("org.jetbrains.grammarkit", "org.jetbrains.grammarkit.gradle.plugin")
        includeModule("org.jetbrains.intellij.plugins", "gradle-grammarkit-plugin")
      }
    }
  }
}
