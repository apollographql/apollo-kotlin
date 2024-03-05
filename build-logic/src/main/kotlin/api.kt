
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask

fun Project.apolloLibrary(
    javaModuleName: String?,
    jvmTarget: Int? = null,
    withJs: Boolean = true,
    withLinux: Boolean = true,
    withApple: Boolean = true,
    withJvm: Boolean = true,
    withWasm: Boolean = true,
    publish: Boolean = true
) {
  group = property("GROUP")!!
  version = property("VERSION_NAME")!!

  commonSetup()
  configureJavaAndKotlinCompilers(jvmTarget)

  addOptIn(
      "com.apollographql.apollo3.annotations.ApolloExperimental",
      "com.apollographql.apollo3.annotations.ApolloInternal"
  )

  configureTesting()

  if (publish) {
    configurePublishing()
  }

  // Within the 'tests' project (a composite build), dependencies are automatically substituted to use the project's one.
  // But we don't want this, for example apollo-tooling depends on a published version of apollo-api.
  // So disable this behavior (see https://docs.gradle.org/current/userguide/composite_builds.html#deactivate_included_build_substitutions).
  configurations.all {
    resolutionStrategy.useGlobalDependencySubstitutionRules.set(false)
  }

  if (extensions.findByName("kotlin") is KotlinMultiplatformExtension) {
    configureMpp(
        withJvm = withJvm,
        withJs = withJs,
        browserTest = false,
        withLinux = withLinux,
        appleTargets = if (!withApple) emptySet() else allAppleTargets,
        withAndroid = extensions.findByName("android") != null,
        withWasm = withWasm
    )
  }

  if (javaModuleName != null) {
    tasks.withType(Jar::class.java).configureEach {
      manifest {
        attributes(mapOf("Automatic-Module-Name" to javaModuleName))
      }
    }
  }
}

fun Project.apolloTest(
    withJs: Boolean = true,
    withJvm: Boolean = true,
    appleTargets: Set<String> = setOf(hostTarget),
    browserTest: Boolean = false,
) {
  commonSetup()
  configureJavaAndKotlinCompilers(null)
  addOptIn(
      "com.apollographql.apollo3.annotations.ApolloExperimental",
      "com.apollographql.apollo3.annotations.ApolloInternal",
  )
  configureTesting()

  if (extensions.findByName("kotlin") is KotlinMultiplatformExtension) {
    configureMpp(
        withJvm = withJvm,
        withJs = withJs,
        browserTest = browserTest,
        withLinux = false,
        withAndroid = false,
        appleTargets = appleTargets,
        withWasm = false
    )
  }
}

fun Project.apolloRoot(ciBuild: TaskProvider<Task>) {
  configureWasmCompatibleNode()
  rootSetup(ciBuild)
}

/**
 * See https://youtrack.jetbrains.com/issue/KT-63014
 */
private fun Project.configureWasmCompatibleNode() {
  check(this == rootProject) {
    "Must only be called in root project"
  }
  plugins.withType(NodeJsRootPlugin::class.java).configureEach {
    extensions.getByType(NodeJsRootExtension::class.java).apply {
      version = "21.0.0-v8-canary202309143a48826a08"
      downloadBaseUrl = "https://nodejs.org/download/v8-canary"
    }

    tasks.withType(KotlinNpmInstallTask::class.java).configureEach {
      args.add("--ignore-engines")
    }
  }
}

fun Project.apolloTestRoot() {
  configureWasmCompatibleNode()
}