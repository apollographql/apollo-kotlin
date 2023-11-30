listOf(pluginManagement.repositories, dependencyResolutionManagement.repositories).forEach {
  it.apply {
    mavenCentral()
    mavenLocal()

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
