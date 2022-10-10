import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun Project.configureJavaAndKotlinCompilers(treatWarningsAsErrors: Boolean = false) {
  // For Kotlin JVM projects
  tasks.withType(KotlinCompile::class.java) {
    kotlinOptions {
      freeCompilerArgs = freeCompilerArgs + listOf(
          "-opt-in=kotlin.RequiresOptIn",
          "-opt-in=com.apollographql.apollo3.annotations.ApolloExperimental",
          "-opt-in=com.apollographql.apollo3.annotations.ApolloInternal"
      )
      if (getKotlinPluginVersion() == "1.6.10") {
        // This is a workaround for https://youtrack.jetbrains.com/issue/KT-47000 (fixed in Kotlin 1.6.20)
        // Since we don't use @JvmDefault anywhere, the option has no effect, but suppresses the bogus compiler error
        // See also https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-default/
        // See also https://blog.jetbrains.com/kotlin/2020/07/kotlin-1-4-m3-generating-default-methods-in-interfaces/
        // TODO for v4, set "-Xjvm-default=all" to remove all DefaultImpls
        freeCompilerArgs = freeCompilerArgs + "-Xjvm-default=compatibility"
      }
      apiVersion = "1.5"
      languageVersion = "1.5"
      jvmTarget = "1.8"
    }
  }

  // For Kotlin Multiplatform projects
  (project.extensions.findByName("kotlin")
      as? org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension)?.run {
    sourceSets.all {
      languageSettings.optIn("kotlin.RequiresOptIn")
      languageSettings.optIn("com.apollographql.apollo3.annotations.ApolloExperimental")
      languageSettings.optIn("com.apollographql.apollo3.annotations.ApolloInternal")
    }
  }

  @Suppress("UnstableApiUsage")
  project.extensions.getByType(JavaPluginExtension::class.java).apply {
    // Compile and run tests with Java11
    toolchain.languageVersion.set(JavaLanguageVersion.of(11))
  }
  @Suppress("UnstableApiUsage")
  project.tasks.withType(JavaCompile::class.java).configureEach {
    // Ensure "org.gradle.jvm.version" is set to "8" in Gradle metadata of jvm-only modules.
    options.release.set(8)
  }

  if (treatWarningsAsErrors) treatWarningsAsErrors()
}

private fun Project.treatWarningsAsErrors() {
  tasks.withType(KotlinCompile::class.java) {
    kotlinOptions {
      allWarningsAsErrors = true
    }
  }
}

// Workaround for https://youtrack.jetbrains.com/issue/KT-51970
fun Project.workaroundForIssueKT51970() {
  afterEvaluate {
    afterEvaluate {
      tasks.configureEach {
        if (
            name.startsWith("compile")
            && name.endsWith("KotlinMetadata")
        ) {
          enabled = false
        }
      }
    }
  }
}
