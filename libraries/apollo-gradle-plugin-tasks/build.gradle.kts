import org.gradle.api.internal.artifacts.dsl.DefaultArtifactHandler

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.gradleup.gratatouille.tasks")
  id("com.google.devtools.ksp")
  id("org.jetbrains.kotlin.plugin.serialization")
}

apolloLibrary(
    jvmTarget = 11, // Gratatouille requires 11
    namespace = "com.apollographql.apollo.gradle.tasks",
)

dependencies {
  implementation(project(":apollo-compiler"))
  implementation(project(":apollo-tooling"))
  implementation(project(":apollo-ast"))
  implementation(libs.asm)
  implementation(libs.kotlinx.serialization.json)
}

gratatouille {
  codeGeneration {
    /**
     * Workaround for https://github.com/GradleUp/gratatouille/pull/52
     */
    configurations.configureEach {
      if (name == "gratatouilleApiElements") {
        artifacts.configureEach {
          this as ConfigurablePublishArtifact
          if (extension.isNullOrEmpty()) {
            extension = "zip"
          }
        }
      }
    }
    classLoaderIsolation()
  }
}

