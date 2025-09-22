import app.cash.licensee.LicenseeExtension
import app.cash.licensee.UnusedAction
import com.gradleup.librarian.gradle.Librarian
import nmcp.NmcpAggregationExtension
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
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
  val defaultTargets = defaultTargets(
      withJvm = withJvm,
      withJs = withJs,
      withLinux = withLinux,
      appleTargets = if (!withApple) emptySet() else allAppleTargets,
      withAndroid = androidOptions != null,
      withWasm = withWasm
  )

  apolloLibrary(
      namespace,
      jvmTarget,
      defaultTargets,
      androidOptions,
      publish,
      kotlinCompilerOptions
  )
}

fun Project.apolloTest(
    withJs: Boolean = true,
    withJvm: Boolean = true,
    appleTargets: Set<String> = setOf(hostTarget),
    kotlinCompilerOptions: KotlinCompilerOptions = KotlinCompilerOptions(),
    jvmTarget: Int? = null
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

