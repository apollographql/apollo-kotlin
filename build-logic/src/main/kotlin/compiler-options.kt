
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinNativeCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

/**
 */
fun KotlinCommonCompilerOptions.configure(
    optIns: List<String>
) {
  freeCompilerArgs.add("-Xexpect-actual-classes")

  // Suppress "Language version 1.9 is deprecated and its support will be removed in a future version of Kotlin"
  freeCompilerArgs.add("-Xsuppress-version-warnings")

  // Prints internal diagnostic names alongside warnings. This is useful for identifying the DIAGNOSTIC_NAME configured for the -Xwarning-level option.
  freeCompilerArgs.add("-Xrender-internal-diagnostic-names")

  // Workaround for https://youtrack.jetbrains.com/projects/KT/issues/KT-84767/
  // TODO remove when that issue is resolved, to avoid being too broad in what's disabled
//  freeCompilerArgs.add("-Xwarning-level=COMPILER_ARGUMENTS_WARNING:disabled")

  optIns.forEach {
    freeCompilerArgs.add("-opt-in=$it")
  }

  when (this) {
    is KotlinJvmCompilerOptions -> {
      freeCompilerArgs.add("-jvm-default=no-compatibility")
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


private fun KotlinProjectExtension.forEachCompilerOptions(block: (KotlinCommonCompilerOptions) -> Unit) {
  when (this) {
    is KotlinJvmProjectExtension -> block(compilerOptions)
    is KotlinAndroidProjectExtension -> block(compilerOptions)
    is KotlinMultiplatformExtension -> {
      targets.all {
        it.compilations.all {
          it.compileTaskProvider.configure {
            it.compilerOptions {
              block(this)
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
    return extensions.findByName("kotlin") as? KotlinProjectExtension
  }

fun Project.configureJavaAndKotlinCompilers(optIns: List<String>) {
  kotlinExtensionOrNull?.forEachCompilerOptions {
    it.configure(optIns)
  }

  allWarningsAsErrors(true)
}

fun Project.allWarningsAsErrors(allWarningsAsErrors: Boolean) {
  kotlinExtensionOrNull?.forEachCompilerOptions {
    it.allWarningsAsErrors.set(allWarningsAsErrors)
  }
}
