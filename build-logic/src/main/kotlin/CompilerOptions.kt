import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile

fun Project.configureJavaAndKotlinCompilers() {
  // For Kotlin JVM projects
  tasks.withType(KotlinCompile::class.java).configureEach {
    kotlinOptions {
      freeCompilerArgs = freeCompilerArgs + listOf(
          "-opt-in=kotlin.RequiresOptIn",
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

      (this as? KotlinJvmOptions)?.let {
        it.jvmTarget = "1.8"
      }
    }
  }
  tasks.withType(KotlinNativeCompile::class.java).configureEach {
    kotlinOptions {
      this.freeCompilerArgs += "-opt-in=kotlinx.cinterop.UnsafeNumber"
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
    // Keep in sync with build-logic/build.gradle.kts
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
  }
  @Suppress("UnstableApiUsage")
  project.tasks.withType(JavaCompile::class.java).configureEach {
    // Ensure "org.gradle.jvm.version" is set to "8" in Gradle metadata of jvm-only modules.
    options.release.set(8)
  }

  allWarningsAsErrors(true)
}

fun Project.configureTests(jvmVersion: Int) {
  tasks.withType(Test::class.java).configureEach {
    val javaToolchains = this@configureTests.extensions.getByName("javaToolchains") as JavaToolchainService
    javaLauncher.set(javaToolchains.launcherFor {
      languageVersion.set(JavaLanguageVersion.of(jvmVersion))
    })
  }
}

internal fun Project.optIn(vararg annotations: String) {
  tasks.withType(KotlinCompile::class.java).configureEach {
    kotlinOptions {
      freeCompilerArgs = freeCompilerArgs + annotations.map { "-opt-in=$it" }
    }
  }
}

fun Project.allWarningsAsErrors(allWarningsAsErrors: Boolean) {
  tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java).configureEach {
    kotlinOptions {
      if (this@configureEach.name.endsWith("KotlinMetadata")) {
        /**
         * KotlinMetadata compilations trigger warnings such as below:
         *
         * w: Could not find "co.touchlab:sqliter-driver-cinterop-sqlite3" in [/Users/mbonnin/git/reproducer-apple-metadata, /Users/mbonnin/.konan/klib, /Users/mbonnin/.konan/kotlin-native-prebuilt-macos-aarch64-1.8.0/klib/common, /Users/mbonnin/.konan/kotlin-native-prebuilt-macos-aarch64-1.8.0/klib/platform/macos_arm64]
         *
         * I'm thinking it has to do with HMPP but not 100% yet. Ignore all warnings in these tasks
         */
        this.allWarningsAsErrors = false
      } else {
        this.allWarningsAsErrors = allWarningsAsErrors
      }
    }
  }
}
