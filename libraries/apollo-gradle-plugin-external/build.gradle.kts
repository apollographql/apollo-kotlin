import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.google.devtools.ksp")
  id("com.gradleup.gratatouille")
}

apolloLibrary(
    namespace = "com.apollographql.apollo.gradle.external",
    description = "Tombstone for apollo-gradle-plugin-external",
    jvmTarget = 11, // To compile against AGP 8.0.0
    kotlinCompilerOptions = KotlinCompilerOptions(KotlinVersion.KOTLIN_1_9) // For better Gradle compatibility
)

gratatouille {
  codeGeneration {
    addDependencies.set(false)
  }
  pluginMarker("com.apollographql.apollo.external")
}

dependencies {
  compileOnly(libs.gradle.api.min)

  implementation(libs.gratatouille.wiring.runtime)
  implementation(libs.gratatouille.tasks.runtime)

  implementation(project(":apollo-annotations"))
  api(project(":apollo-gradle-plugin"))
}

extensions.getByType(PublishingExtension::class.java).publications.getByName("default").apply {
  this as MavenPublication
  pom {
    distributionManagement {
      relocation {
        artifactId = "apollo-gradle-plugin"
        message = "The `apollo-gradle-plugin-external` artifact and the `com.apollographql.apollo.external` plugin have been removed. The `apollo-gradle-plugin` artifact and `com.apollographql.apollo` plugin use classloader isolation and should be used instead."
      }
    }
  }
}