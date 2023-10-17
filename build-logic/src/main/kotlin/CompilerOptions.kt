import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinNativeCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

fun KotlinCommonCompilerOptions.configure() {
  freeCompilerArgs.add("-Xexpect-actual-classes")

  /**
   * Inside our own codebase, we opt-in ApolloInternal and ApolloExperimental
   * We might want to do something more precise where we only opt-in for libraries but still require integration tests to opt-in with more granularity
   */
  freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
  freeCompilerArgs.add("-opt-in=com.apollographql.apollo3.annotations.ApolloExperimental")
  freeCompilerArgs.add("-opt-in=com.apollographql.apollo3.annotations.ApolloInternal")

  apiVersion.set(KotlinVersion.KOTLIN_1_9)
  languageVersion.set(KotlinVersion.KOTLIN_1_9)

  when (this) {
    is KotlinJvmCompilerOptions -> {
      freeCompilerArgs.add("-Xjvm-default=all")
      jvmTarget.set(JvmTarget.JVM_1_8)
    }

    is KotlinNativeCompilerOptions -> {
      freeCompilerArgs.add("-opt-in=kotlinx.cinterop.UnsafeNumber")
      freeCompilerArgs.add("-opt-in=kotlinx.cinterop.ExperimentalForeignApi")
    }

    is KotlinJsCompilerOptions -> {
      // nothing!
    }
  }
}

private fun KotlinProjectExtension.forEachCompilerOptions(block: KotlinCommonCompilerOptions.() -> Unit) {
  when (this) {
    is KotlinJvmProjectExtension -> compilerOptions.block()
    is KotlinAndroidProjectExtension -> compilerOptions.block()
    is KotlinMultiplatformExtension -> {
      targets.all {
        compilations.all {
          compilerOptions.configure {
            block()
          }
        }
      }
    }
    else -> error("Unknown kotlin extension $this")
  }
}


val Project.kotlinExtensionOrNull: KotlinProjectExtension?
  get() {
    return (extensions.findByName("kotlin") as? KotlinProjectExtension)
  }

fun Project.configureJavaAndKotlinCompilers() {
  kotlinExtensionOrNull?.forEachCompilerOptions {
    configure()
  }

  (kotlinExtensionOrNull as? KotlinMultiplatformExtension)?.sourceSets?.configureEach {
    languageSettings.optIn("com.apollographql.apollo3.annotations.ApolloExperimental")
    languageSettings.optIn("com.apollographql.apollo3.annotations.ApolloInternal")
  }

  @Suppress("UnstableApiUsage")
  project.extensions.getByType(JavaPluginExtension::class.java).apply {
    // Keep in sync with build-logic/build.gradle.kts
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
  }
  project.tasks.withType(JavaCompile::class.java).configureEach {
    // Ensure "org.gradle.jvm.version" is set to "8" in Gradle metadata of jvm-only modules.
    options.release.set(8)
  }

  // https://youtrack.jetbrains.com/issue/KT-62653
  // allWarningsAsErrors(true)
}

@Suppress("UnstableApiUsage")
fun setTestToolchain(project: Project, test: Test, javaVersion: Int) {
  val javaToolchains = project.extensions.getByName("javaToolchains") as JavaToolchainService
  test.javaLauncher.set(javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(javaVersion))
  })

}

internal fun Project.addOptIn(vararg annotations: String) {
  tasks.withType(KotlinCompile::class.java).configureEach {
    kotlinOptions {
      freeCompilerArgs = freeCompilerArgs + annotations.map { "-opt-in=$it" }
    }
  }
}

fun Project.allWarningsAsErrors(allWarningsAsErrors: Boolean) {
  kotlinExtensionOrNull?.forEachCompilerOptions {
    this.allWarningsAsErrors.set(allWarningsAsErrors)
  }
}
