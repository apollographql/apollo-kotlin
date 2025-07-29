listOf(pluginManagement.repositories, dependencyResolutionManagement.repositories).forEach {
  it.apply {
    // Uncomment this one to use the Kotlin "dev" repository
    maven("https://redirector.kotlinlang.org/maven/dev/")
    // Uncomment this one to use the Sonatype OSSRH snapshots repository
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
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
      forRepository { maven("https://storage.googleapis.com/gradleup/m2") }
      filter {
        includeGroup("com.gradleup.librarian")
      }
    }

    exclusiveContent {
      forRepository(::gradlePluginPortal)
      filter {
        includeModule("org.jetbrains.kotlinx", "kotlinx-benchmark-plugin")
        includeModule("com.github.ben-manes", "gradle-versions-plugin")
        includeModule("com.gradle", "develocity-gradle-plugin")
      }
    }
  }
}
