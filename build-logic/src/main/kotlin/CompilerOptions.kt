import com.android.build.gradle.BaseExtension
import compat.patrouille.configureJavaCompatibility
import compat.patrouille.configureKotlinCompatibility
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

/**
 * @param target the JVM version we want to be compatible with (bytecode + bootstrap classpath)
 */
fun KotlinCommonCompilerOptions.configure(
    isAndroid: Boolean,
    optIns: List<String>
) {
  freeCompilerArgs.add("-Xexpect-actual-classes")

  optIns.forEach {
    freeCompilerArgs.add("-opt-in=$it")
  }

  when (this) {
    is KotlinJvmCompilerOptions -> {
      freeCompilerArgs.add("-Xjvm-default=all")
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

fun Project.configureJavaAndKotlinCompilers(jvmTarget: Int?, kotlinCompilerOptions: KotlinCompilerOptions, optIns: List<String>) {
  @Suppress("NAME_SHADOWING")
  val jvmTarget = jvmTarget ?: 8

  configureJavaCompatibility(jvmTarget)
  if (kotlinExtensionOrNull != null) {
    configureKotlinCompatibility(kotlinCompilerOptions.version.version + ".0")
  }
  kotlinExtensionOrNull?.forEachCompilerOptions { isAndroid ->
    configure(isAndroid, optIns)
  }

  allWarningsAsErrors(true)
}

fun setTestToolchain(project: Project, test: Test, javaVersion: Int) {
  val javaToolchains = project.extensions.getByName("javaToolchains") as JavaToolchainService
  test.javaLauncher.set(javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(javaVersion))
  })

}

fun Project.allWarningsAsErrors(allWarningsAsErrors: Boolean) {
  kotlinExtensionOrNull?.forEachCompilerOptions {
    this.allWarningsAsErrors.set(allWarningsAsErrors)
  }
}
