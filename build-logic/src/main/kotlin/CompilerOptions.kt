
import com.android.build.gradle.BaseExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinNativeCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

/**
 * @param target the JVM version we want to be compatible with (bytecode + bootstrap classpath)
 */
fun KotlinCommonCompilerOptions.configure(target: Int, kotlinCompilerOptions: KotlinCompilerOptions, isAndroid: Boolean) {
  val actualTarget = when {
    isAndroid -> {
      // https://blog.blundellapps.co.uk/setting-jdk-level-in-android-gradle-builds/
      // D8 can dex Java17 bytecode
      17
    }
    else -> target
  }

  freeCompilerArgs.add("-Xexpect-actual-classes")

  /**
   * Inside our own codebase, we opt-in ApolloInternal and ApolloExperimental
   * We might want to do something more precise where we only opt-in for libraries but still require integration tests to opt-in with more granularity
   */
  freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
  freeCompilerArgs.add("-opt-in=com.apollographql.apollo.annotations.ApolloExperimental")
  freeCompilerArgs.add("-opt-in=com.apollographql.apollo.annotations.ApolloInternal")

  apiVersion.set(kotlinCompilerOptions.version)
  languageVersion.set(kotlinCompilerOptions.version)

  when (this) {
    is KotlinJvmCompilerOptions -> {
      freeCompilerArgs.add("-Xjvm-default=all")
      if (!isAndroid) {
        // See https://cs.android.com/android-studio/platform/tools/base/+/mirror-goog-studio-main:build-system/gradle-core/src/main/java/com/android/build/gradle/tasks/JavaCompileUtils.kt;l=410?q=Using%20%27--release%27%20option%20for%20JavaCompile%20is%20not%20supported%20because%20it%20prevents%20the%20Android%20Gradle%20plugin
        freeCompilerArgs.add("-Xjdk-release=${actualTarget.toJvmTarget().target}")
      }
      jvmTarget.set(actualTarget.toJvmTarget())
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

private fun Int.toJvmTarget(): JvmTarget {
  return when(this) {
    8 -> JvmTarget.JVM_1_8
    else -> JvmTarget.fromTarget(this.toString())
  }
}

private fun KotlinProjectExtension.forEachCompilerOptions(block: KotlinCommonCompilerOptions.(isAndroid: Boolean) -> Unit) {
  when (this) {
    is KotlinJvmProjectExtension -> compilerOptions.block(false)
    is KotlinAndroidProjectExtension -> compilerOptions.block(true)
    is KotlinMultiplatformExtension -> {
      targets.all {
        val isAndroid = platformType == KotlinPlatformType.androidJvm
        compilations.all {
          compileTaskProvider.configure {
            compilerOptions {
              block(isAndroid)
            }
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

val Project.androidExtensionOrNull: BaseExtension?
  get() {
    return (extensions.findByName("android") as? BaseExtension)
  }

fun Project.configureJavaAndKotlinCompilers(jvmTarget: Int?, kotlinCompilerOptions: KotlinCompilerOptions) {
  @Suppress("NAME_SHADOWING")
  val jvmTarget = jvmTarget?: 8

  kotlinExtensionOrNull?.forEachCompilerOptions { isAndroid ->
    configure(jvmTarget, kotlinCompilerOptions, isAndroid)
  }
  project.tasks.withType(JavaCompile::class.java).configureEach {
    // For JVM only modules, this dictates the "org.gradle.jvm.version" Gradle attribute
    options.release.set(jvmTarget)
  }
  androidExtensionOrNull?.run {
    compileOptions {
      // For Android, latest D8 version support Java 17
      targetCompatibility = JavaVersion.VERSION_17
      sourceCompatibility = JavaVersion.VERSION_17
    }
  }

  (kotlinExtensionOrNull as? KotlinMultiplatformExtension)?.sourceSets?.configureEach {
    languageSettings.optIn("com.apollographql.apollo.annotations.ApolloExperimental")
    languageSettings.optIn("com.apollographql.apollo.annotations.ApolloInternal")
  }

  /**
   * We're using a toolchain to ensure build cache can be shared. Many tasks, including compileJava use the
   * java version as input and using different JDKs will trash the cache.
   * Note: It's unclear how the java version actually changes the behaviour of a task. We might be able to
   * remove this in the future if compileJava and others remove it from their inputs.
   * See https://issuetracker.google.com/issues/283097109
   */
  project.extensions.getByType(JavaPluginExtension::class.java).apply {
    // Keep in sync with build-logic/build.gradle.kts
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
  }

  /**
   * Required because of:
   *
   * > Task :apollo-runtime:compileKotlinWasmJs
   * w: duplicate library name: kotlin
   *
   * (maybe https://youtrack.jetbrains.com/issue/KT-51110?)
   */
  allWarningsAsErrors(false)
}

fun setTestToolchain(project: Project, test: Test, javaVersion: Int) {
  val javaToolchains = project.extensions.getByName("javaToolchains") as JavaToolchainService
  test.javaLauncher.set(javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(javaVersion))
  })

}

internal fun Project.addOptIn(vararg annotations: String) {
  tasks.withType(KotlinCompilationTask::class.java).configureEach {
    compilerOptions {
      freeCompilerArgs.addAll(annotations.map { "-opt-in=$it" })
    }
  }
}

fun Project.allWarningsAsErrors(allWarningsAsErrors: Boolean) {
  kotlinExtensionOrNull?.forEachCompilerOptions {
    this.allWarningsAsErrors.set(allWarningsAsErrors)
  }
}
