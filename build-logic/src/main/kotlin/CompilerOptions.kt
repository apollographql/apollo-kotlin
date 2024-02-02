
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
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinNativeCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

fun KotlinCommonCompilerOptions.configure(baseJvmTarget: Int, isAndroid: Boolean) {
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
      val target = when {
        isAndroid -> {
          // https://blog.blundellapps.co.uk/setting-jdk-level-in-android-gradle-builds/
          // D8 can dex Java17 bytecode
          JvmTarget.JVM_17
        }
        baseJvmTarget == 8 -> JvmTarget.JVM_1_8
        else -> JvmTarget.fromTarget(baseJvmTarget.toString())
      }
      jvmTarget.set(target)
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

private fun KotlinProjectExtension.forEachCompilerOptions(block: KotlinCommonCompilerOptions.(isAndroid: Boolean) -> Unit) {
  when (this) {
    is KotlinJvmProjectExtension -> compilerOptions.block(false)
    is KotlinAndroidProjectExtension -> compilerOptions.block(true)
    is KotlinMultiplatformExtension -> {
      targets.all {
        val isAndroid = platformType == KotlinPlatformType.androidJvm
        compilations.all {
          compilerOptions.configure {
            block(isAndroid)
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

fun Project.configureJavaAndKotlinCompilers(jvmTarget: Int?) {
  @Suppress("NAME_SHADOWING")
  val jvmTarget = jvmTarget?: 8

  kotlinExtensionOrNull?.forEachCompilerOptions {
    configure(jvmTarget, it)
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
    languageSettings.optIn("com.apollographql.apollo3.annotations.ApolloExperimental")
    languageSettings.optIn("com.apollographql.apollo3.annotations.ApolloInternal")
  }

  /**
   * We're using a toolchain to ensure build cache can be shared. Many tasks, including compileJava use the
   * java version as input and using different JDKs will trash the cache.
   * Note: It's unclear how the java version actually changes the behaviour of a task. We might be able to
   * remove this in the future if compileJava and others remove it from their inputs.
   * See https://issuetracker.google.com/issues/283097109
   */
  @Suppress("UnstableApiUsage")
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
