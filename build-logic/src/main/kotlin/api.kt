import app.cash.licensee.LicenseeExtension
import app.cash.licensee.UnusedAction
import com.gradleup.librarian.gradle.Librarian
import nmcp.NmcpAggregationExtension
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.ExecutionTaskHolder
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithSimulatorTests
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithTests
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeOutputKind
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

class AndroidOptions(
    val withCompose: Boolean,
)

class KotlinCompilerOptions(
    val version: KotlinVersion = KotlinVersion.KOTLIN_2_0,
)

fun Project.version(): String {
  return Librarian.version(property("VERSION_NAME")!!.toString())
}

fun Project.apolloLibrary(
    namespace: String,
    jvmTarget: Int? = null,
    defaultTargets: (KotlinMultiplatformExtension.() -> Unit),
    androidOptions: AndroidOptions? = null,
    publish: Boolean = true,
    kotlinCompilerOptions: KotlinCompilerOptions = KotlinCompilerOptions(),
    contributesCtng: Boolean = true,
    enableWasmJsTests: Boolean = true
) {
  group = property("GROUP")!!
  version = version()

  if (androidOptions != null) {
    configureAndroid(namespace, androidOptions)
  }
  commonSetup()

  configureJavaAndKotlinCompilers(
      jvmTarget,
      kotlinCompilerOptions,
      listOf(
          "kotlin.RequiresOptIn",
          "com.apollographql.apollo.annotations.ApolloInternal",
          "com.apollographql.apollo.annotations.ApolloExperimental"
      )
  )

  if (publish) {
    configurePublishing()
  }

  configurations.configureEach {
    if (name == "apolloPublished" || name.matches(Regex("apollo.*Compiler"))) {
      // Within the 'tests' project (a composite build), dependencies are automatically substituted to use the project's one.
      // apollo-tooling depends on a published version of apollo-api which should not be substituted for both the runtime
      // and compiler classpaths.
      // See (see https://docs.gradle.org/current/userguide/composite_builds.html#deactivate_included_build_substitutions).
      this.resolutionStrategy.useGlobalDependencySubstitutionRules.set(false)
    }
  }

  if (extensions.findByName("kotlin") is KotlinMultiplatformExtension) {
    val kotlinExtension = extensions.findByName("kotlin") as? KotlinMultiplatformExtension
    if (kotlinExtension != null) {
      kotlinExtension.defaultTargets()
      kotlinExtension.configureSourceSetGraph()
    }
  }

  configureTesting()
  project.kotlinTargets.forEach { target ->
    /**
     * Disable every native test except the KotlinNativeTargetWithHostTests to save some time
     */
    if (target is KotlinNativeTargetWithSimulatorTests || target is KotlinNativeTargetWithTests<*>) {
      target.testRuns.configureEach {
        this as ExecutionTaskHolder<*>
        executionTask.configure {
          enabled = false
        }
      }
      target.binaries.configureEach {
        if (outputKind == NativeOutputKind.TEST) {
          linkTaskProvider.configure {
            enabled = false
          }
          compilation.compileTaskProvider.configure {
            enabled = false
          }
        }
      }
    }
    /**
     * Disable wasmJs tests because they are not ready yet
     */
    if (!enableWasmJsTests && target is KotlinJsIrTarget && target.wasmTargetType != null) {
      target.subTargets.configureEach {
        testRuns.configureEach {
          executionTask.configure {
            enabled = false
          }
        }
      }
      target.testRuns.configureEach {
        executionTask.configure {
          enabled = false
        }
      }
      target.binaries.configureEach {
        compilation.compileTaskProvider.configure {
          enabled = false
        }
      }
    }
  }
  /**
   * `ctng` is short for CiTestNoGradle. It's a shorthand task that runs all the `build`
   * tasks except the Gradle plugin one because it is slow.
   * the name is for historical reasons.
   */
  tasks.register("ctng") {
    if (contributesCtng) {
      dependsOn("build")
    }
  }

  tasks.withType(Jar::class.java).configureEach {
    manifest {
      attributes(mapOf("Automatic-Module-Name" to namespace))
    }
  }

  plugins.apply("app.cash.licensee")
  extensions.getByType(LicenseeExtension::class.java).apply {
    unusedAction(UnusedAction.IGNORE)

    allow("Apache-2.0")
    allow("MIT")
    allow("MIT-0")
    allow("CC0-1.0")

    // remove when https://github.com/apollographql/apollo-kotlin-execution/pull/64 is released
    allowDependency("com.apollographql.execution", "apollo-execution-runtime", "0.1.1")
    allowDependency("com.apollographql.execution", "apollo-execution-runtime-jvm", "0.1.1")

    allowUrl("https://asm.ow2.io/license.html")
    allowUrl("https://spdx.org/licenses/MIT.txt")
  }
}

private val Project.kotlinTargets: Collection<KotlinTarget>
  get() {
    when (val kotlin = extensions.getByName("kotlin")) {
      is KotlinJvmExtension -> return listOf(kotlin.target)
      is KotlinMultiplatformExtension -> return kotlin.targets
      else -> return emptyList()
    }
  }

fun Project.apolloLibrary(
    namespace: String,
    jvmTarget: Int? = null,
    withJs: Boolean = true,
    withLinux: Boolean = true,
    withApple: Boolean = true,
    withJvm: Boolean = true,
    withWasm: Boolean = true,
    androidOptions: AndroidOptions? = null,
    publish: Boolean = true,
    kotlinCompilerOptions: KotlinCompilerOptions = KotlinCompilerOptions(),
    contributesCtng: Boolean = true,
    enableWasmJsTests: Boolean = true,
) {
  val defaultTargets = defaultTargets(
      withJvm = withJvm,
      withJs = withJs,
      withLinux = withLinux,
      appleTargets = if (!withApple) emptySet() else allAppleTargets,
      withAndroid = androidOptions != null,
      withWasm = withWasm
  )

  apolloLibrary(
      namespace = namespace,
      jvmTarget = jvmTarget,
      defaultTargets = defaultTargets,
      androidOptions = androidOptions,
      publish = publish,
      kotlinCompilerOptions = kotlinCompilerOptions,
      contributesCtng = contributesCtng,
      enableWasmJsTests = enableWasmJsTests
  )
}

fun Project.apolloTest(
    withJs: Boolean = true,
    withJvm: Boolean = true,
    appleTargets: Set<String> = setOf(hostTarget),
    kotlinCompilerOptions: KotlinCompilerOptions = KotlinCompilerOptions(),
    jvmTarget: Int? = null,
) {
  apolloTest(
      kotlinCompilerOptions = kotlinCompilerOptions,
      jvmTarget = jvmTarget,
      block = defaultTargets(withJvm = withJvm, withJs = withJs, withLinux = false, withAndroid = false, withWasm = false, appleTargets = appleTargets),
  )
}

fun Project.apolloTest(
    kotlinCompilerOptions: KotlinCompilerOptions = KotlinCompilerOptions(),
    jvmTarget: Int? = null,
    block: KotlinMultiplatformExtension.() -> Unit,
) {
  commonSetup()
  configureJavaAndKotlinCompilers(
      jvmTarget,
      kotlinCompilerOptions,
      listOf(
          "kotlin.RequiresOptIn",
          "com.apollographql.apollo.annotations.ApolloExperimental",
          "com.apollographql.apollo.annotations.ApolloInternal"
      )
  )

  val kotlinExtension = extensions.findByName("kotlin") as? KotlinMultiplatformExtension
  if (kotlinExtension != null) {
    kotlinExtension.block()
    kotlinExtension.configureSourceSetGraph()
  }
  configureTesting()
}

fun Project.apolloRoot() {
  configureNode()
  rootSetup()

  pluginManager.apply("com.gradleup.nmcp.aggregation")
  val nmcpAggregation = extensions.getByType(NmcpAggregationExtension::class.java)
  nmcpAggregation.apply {
    centralPortal {
      username.set(System.getenv("LIBRARIAN_SONATYPE_USERNAME"))
      password.set(System.getenv("LIBRARIAN_SONATYPE_PASSWORD"))
      validationTimeout.set(30.minutes.toJavaDuration())
      publishingTimeout.set(1.hours.toJavaDuration())
    }
  }

  Librarian.registerGcsTask(
      this,
      provider { "apollo-previews" },
      provider { "m2" },
      provider { System.getenv("LIBRARIAN_GOOGLE_SERVICES_JSON") },
      nmcpAggregation.allFiles
  )

  subprojects.forEach {
    configurations.getByName("nmcpAggregation").dependencies.add(dependencies.create(it))
  }
}

