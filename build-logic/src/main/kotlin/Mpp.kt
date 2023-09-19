import org.gradle.api.Action
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrLink
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import org.jetbrains.kotlin.konan.target.Family

private val allAppleTargets = setOf(
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

fun Project.configureMppDefaults(withJs: Boolean, withLinux: Boolean, withAndroid: Boolean) {
  configureMpp(
      withJvm = true,
      withJs = withJs,
      browserTest = false,
      withLinux = withLinux,
      appleTargets = allAppleTargets,
      withAndroid = withAndroid,
  )
}


/**
 * Same as [configureMppDefaults] but without Linux targets. Apple targets can be configured.
 * Tests only run on the JVM, JS and macOS
 */
fun Project.configureMppTestsDefaults(
    withJs: Boolean,
    withJvm: Boolean,
    browserTest: Boolean,
    appleTargets: Collection<String>,
) {
  configureMpp(
      withJvm = withJvm,
      withJs = withJs,
      browserTest = browserTest,
      withLinux = false,
      withAndroid = false,
      appleTargets = appleTargets,
  )
}

fun Project.configureMpp(
    withJvm: Boolean,
    withJs: Boolean,
    withLinux: Boolean,
    withAndroid: Boolean,
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

    configureSourceSetGraph()
    addTestDependencies()

    tasks.withType(KotlinJsIrLink::class.java).configureEach {
      notCompatibleWithConfigurationCache("https://youtrack.jetbrains.com/issue/KT-60311/")
    }
    tasks.withType(KotlinNativeLink::class.java).configureEach {
      notCompatibleWithConfigurationCache("https://youtrack.jetbrains.com/issue/KT-60311/")
    }
  }
}

/**
 * Current Graph is something like so:
 *
 * ```mermaid
 * graph TB
 * commonMain --> concurrentMain
 * commonMain --> linuxMain
 * commonMain --> jsMain
 * concurrentMain --> jvmMain
 * concurrentMain --> appleMain
 * appleMain --> macosX64
 * appleMain --> macosArm64
 * appleMain --> iosArm64
 * appleMain --> iosX64
 * appleMain --> iosSimulatorArm64
 * appleMain --> watchosArm32
 * appleMain --> watchosArm64
 * appleMain --> watchosSimulatorArm64
 * appleMain --> tvosArm64
 * appleMain --> tvosX64
 * appleMain --> tvosSimulatorArm64
 *
 * classDef kotlinPurple fill:#A97BFF,stroke:#333,stroke-width:2px,color:#333
 * classDef javaOrange fill:#b07289,stroke:#333,stroke-width:2px,color:#333
 * classDef gray fill:#AAA,stroke:#333,stroke-width:2px,color:#333
 * class jvmJavaCodeGen,macOsArm64Test,jvmTest,jsTest kotlinPurple
 * class commonTest javaOrange
 * ```
 */
private fun KotlinMultiplatformExtension.configureSourceSetGraph() {
  val hasAppleTarget = targets.any {
    it is KotlinNativeTarget && it.konanTarget.family in setOf(Family.IOS, Family.OSX, Family.WATCHOS, Family.TVOS)
  }

  val concurrentMain = sourceSets.create("concurrentMain")
  val concurrentTest = sourceSets.create("concurrentTest")

  concurrentMain.dependsOn(sourceSets.getByName("commonMain"))
  concurrentTest.dependsOn(sourceSets.getByName("commonTest"))

  sourceSets.findByName("jvmMain")?.dependsOn(concurrentMain)
  sourceSets.findByName("jvmTest")?.dependsOn(concurrentTest)

  if (hasAppleTarget) {
    val appleMain = sourceSets.create("appleMain")
    val appleTest = sourceSets.create("appleTest")

    appleMain.dependsOn(concurrentMain)
    appleTest.dependsOn(concurrentTest)

    allAppleTargets.forEach {
      sourceSets.findByName("${it}Main")?.dependsOn(appleMain)
      sourceSets.findByName("${it}Test")?.dependsOn(appleTest)
    }
  }
}

private fun KotlinMultiplatformExtension.addTestDependencies() {
  sourceSets.getByName("commonTest") {
    dependencies {
      implementation(kotlin("test"))
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
