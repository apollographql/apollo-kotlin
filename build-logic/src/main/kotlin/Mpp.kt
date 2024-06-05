
import org.gradle.api.Action
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

internal val allAppleTargets = setOf(
    "macosX64",
    "macosArm64",
    "iosArm64",
    "iosX64",
    "iosSimulatorArm64",
    "watchosArm32",
    "watchosArm64",
    "watchosSimulatorArm64",
    "tvosArm64",
    "tvosX64",
    "tvosSimulatorArm64",
)

// Try to guess the dev machine to make sure the tests are running smoothly
internal val hostTarget: String
  get() = if (System.getProperty("os.arch") == "aarch64") {
    "macosArm64"
  } else {
    "macosX64"
  }

private val enableLinux = System.getenv("APOLLO_JVM_ONLY")?.toBoolean()?.not() ?: true
private val enableJs = System.getenv("APOLLO_JVM_ONLY")?.toBoolean()?.not() ?: true
private val enableApple = System.getenv("APOLLO_JVM_ONLY")?.toBoolean()?.not() ?: true


fun Project.configureMpp(
    withJvm: Boolean,
    withJs: Boolean,
    withLinux: Boolean,
    withAndroid: Boolean,
    withWasm: Boolean,
    appleTargets: Collection<String>,
    browserTest: Boolean,
) {
  val kotlinExtension = extensions.findByName("kotlin") as? KotlinMultiplatformExtension
  check(kotlinExtension != null) {
    "No multiplatform extension found"
  }
  kotlinExtension.apply {
    if (withJvm) {
      jvm()
    }

    if (enableJs && withJs) {
      js(IR) {
        if (browserTest) {
          browser {
            testTask(Action {
              useKarma {
                useChromeHeadless()
              }
            })
          }
        } else {
          nodejs {
            testTask(Action {
              useMocha {
                // Override default 2s timeout
                timeout = "120s"
              }
            })
          }
        }
      }
    }

    if (enableLinux && withLinux) {
      linuxX64("linux")
    }

    if (withAndroid) {
      androidTarget {
        publishAllLibraryVariants()
      }
    }

    if (enableApple) {
      appleTargets.toSet().intersect(allAppleTargets).forEach { presetName ->
        when (presetName) {
          "macosX64" -> macosX64()
          "macosArm64" -> macosArm64()
          "iosArm64" -> iosArm64()
          "iosX64" -> iosX64()
          "iosSimulatorArm64" -> iosSimulatorArm64()
          "watchosArm32" -> watchosArm32()
          "watchosArm64" -> watchosArm64()
          "watchosSimulatorArm64" -> watchosSimulatorArm64()
          "tvosArm64" -> tvosArm64()
          "tvosX64" -> tvosX64()
          "tvosSimulatorArm64" -> tvosSimulatorArm64()
        }
      }
    }
    if (withWasm) {
      @OptIn(ExperimentalWasmDsl::class)
      wasmJs {
        /**
         * See https://youtrack.jetbrains.com/issue/KT-63014
         */
        nodejs()
      }
    }

    configureSourceSetGraph()
  }
}

/**
 * Current Graph is something like so:
 *
 * ```mermaid
 * graph
 * common --> fileSystem
 * fileSystem --> concurrent
 * fileSystem --> jsCommon
 * concurrent --> native
 * concurrent --> jvmCommon
 * jsCommon --> js
 * jsCommon --> wasmJs
 * jvmCommon --> jvm
 * jvmCommon --> android
 * native --> linux
 * native --> apple
 * apple --> macosX64
 * apple --> macosArm64
 * apple --> iosArm64
 * apple --> iosX64
 * apple --> iosSimulatorArm64
 * apple --> watchosArm32
 * apple --> watchosArm64
 * apple --> watchosSimulatorArm64
 * apple --> tvosArm64
 * apple --> tvosX64
 * apple --> tvosSimulatorArm64
 * ```
 */
@OptIn(ExperimentalKotlinGradlePluginApi::class)
private fun KotlinMultiplatformExtension.configureSourceSetGraph() {
  applyDefaultHierarchyTemplate {
    group("common") {
      group("filesystem") {
        group("concurrent") {
          group("native") {
            group("apple")
          }
          group("jvmCommon") {
            withJvm()
            withAndroidTarget()
          }
        }
        group("jsCommon") {
          group("js") {
            withJs()
          }
          group("wasmJs") {
            withWasmJs()
          }
        }
      }
    }
  }
}


/**
 * Registers a new testRun that substitutes the Kotlin models by the Java models.
 * Because they have the same JVM API, this is transparent to all tests that are in `commonTest` that work the same for Kotlin
 * & Java models.
 *
 * - For Java models, we create a separate compilation (and therefore sourceSet graph). The generated models are wired to the separate
 * `commonJavaCodegenTest` source set. Then the contents of `commonTest/kotlin` is sourced directly
 *
 * This breaks IDE support because now `commonTest/kotlin` is used from 2 different places so clicking a model there, it's impossible
 * to tell which model it is. We could expect/actual all of the model APIs but that'd be a lot of very manual work
 */
fun Project.registerJavaCodegenTestTask() {
  val kotlin = kotlinExtension
  check(kotlin is KotlinMultiplatformExtension) {
    "Only multiplatform projects can register a javaCodegenTest task"
  }
  val jvmTarget = kotlin.targets.getByName("jvm") as KotlinJvmTarget
  jvmTarget.withJava()

  /**
   * This is an intermediate source set to make sure that we do not have expect/actual
   * in the same Kotlin module
   */
  val commonJavaCodegenTest = kotlin.sourceSets.create("commonJavaCodegenTest") {
    this.kotlin.srcDir("src/commonTest/kotlin")
  }
  val javaCodegenCompilation = jvmTarget.compilations.create("javaCodegenTest")

  val testRun = jvmTarget.testRuns.create("javaCodegen")
  testRun.setExecutionSourceFrom(javaCodegenCompilation)

  javaCodegenCompilation.compileJavaTaskProvider?.configure {
    classpath += configurations.getByName("jvmTestCompileClasspath")
  }
  javaCodegenCompilation.configurations.compileDependencyConfiguration.extendsFrom(configurations.getByName("jvmTestCompileClasspath"))
  javaCodegenCompilation.configurations.runtimeDependencyConfiguration?.extendsFrom(configurations.getByName("jvmTestRuntimeClasspath"))
  javaCodegenCompilation.defaultSourceSet.dependsOn(commonJavaCodegenTest)
}
