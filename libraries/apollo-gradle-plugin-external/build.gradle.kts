import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.google.devtools.ksp")
  id("com.gradleup.gratatouille")
}

apolloLibrary(
    namespace = "com.apollographql.apollo.gradle.external",
    jvmTarget = 11, // To compile against AGP 8.0.0
    kotlinCompilerOptions = KotlinCompilerOptions(KotlinVersion.KOTLIN_1_9) // For better Gradle compatibility
)

gratatouille {
  codeGeneration()
  pluginMarker("com.apollographql.apollo.external")
}

dependencies {
  compileOnly(libs.gradle.api.min)
  compileOnly(libs.kotlin.plugin.min)

  implementation(project(":apollo-annotations"))
  api(project(":apollo-gradle-plugin"))
}

extensions.getByType(PublishingExtension::class.java).publications.getByName("default").apply {
  this as MavenPublication
  pom {
    distributionManagement {
      relocation {
        artifactId = "apollo-gradle-plugin"
        message = "The Apollo Gradle Plugin now uses classloader isolation and does not use R8 to relocate dependencies anymore. As a result, the `apollo-gradle-plugin-external` artifact and the `com.apollographql.apollo.external` plugins have been removed. You should use `apollo-gradle-plugin` and `com.apollographql.apollo` instead."
      }
    }
  }
}