import app.cash.licensee.LicenseeExtension
import app.cash.licensee.UnusedAction
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

class AndroidOptions(
    val withCompose: Boolean,
)

class KotlinCompilerOptions(
    val version: KotlinVersion = KotlinVersion.KOTLIN_2_0,
)

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
) {
  group = property("GROUP")!!
  version = property("VERSION_NAME")!!

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

  configureTesting()

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
    allow("CC0-1.0")
    allow("MIT-0")

    allowUrl("https://raw.githubusercontent.com/apollographql/apollo-kotlin-execution/main/LICENSE")
    allowUrl("https://raw.githubusercontent.com/apollographql/apollo-kotlin-mockserver/main/LICENSE")
    allowUrl("https://raw.githubusercontent.com/apollographql/apollo-kotlin/main/LICENSE")
    allowUrl("https://asm.ow2.io/license.html")
    allowUrl("https://spdx.org/licenses/MIT.txt")
  }
}

fun Project.apolloTest(
    withJs: Boolean = true,
    withJvm: Boolean = true,
    appleTargets: Set<String> = setOf(hostTarget),
    browserTest: Boolean = false,
    kotlinCompilerOptions: KotlinCompilerOptions = KotlinCompilerOptions(),
) {
  commonSetup()
  configureJavaAndKotlinCompilers(
      null,
      kotlinCompilerOptions,
      listOf(
          "kotlin.RequiresOptIn",
          "com.apollographql.apollo.annotations.ApolloExperimental",
          "com.apollographql.apollo.annotations.ApolloInternal"
      )
  )

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
  configureTesting()
}

fun Project.apolloRoot(ciBuild: TaskProvider<Task>) {
  configureNode()
  rootSetup(ciBuild)
}

