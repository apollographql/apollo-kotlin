import com.gradleup.librarian.gradle.Bcv
import com.gradleup.librarian.gradle.Coordinates
import com.gradleup.librarian.gradle.Gcs
import com.gradleup.librarian.gradle.Kdoc
import com.gradleup.librarian.gradle.Librarian
import com.gradleup.librarian.gradle.PomMetadata
import com.gradleup.librarian.gradle.Publishing
import com.gradleup.librarian.gradle.Signing
import com.gradleup.librarian.gradle.Sonatype
import com.gradleup.librarian.gradle.librarianModule
import com.gradleup.librarian.gradle.librarianRoot
import compat.patrouille.configureJavaCompatibility
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationVariantSpec
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

private val groupId = "com.apollographql.apollo"
private val githubUrl = "https://github.com/apollographql/apollo-kotlin/"
private val license = "MIT"
private val developer = "Apollo"

class AndroidOptions(
    val withCompose: Boolean,
)

class KotlinCompilerOptions(
    val version: KotlinVersion = KotlinVersion.KOTLIN_2_0,
)

fun Project.version(): String {
  return Librarian.version(property("VERSION_NAME")!!.toString())
}

fun group(): String {
  return groupId
}


private fun signing(): Signing {
  return Signing(
      privateKey = System.getenv("GPG_PRIVATE_KEY"),
      privateKeyPassword = System.getenv("GPG_PRIVATE_KEY_PASSWORD")
  )
}

fun Project.apolloLibrary(
    namespace: String,
    jvmTarget: Int?,
    defaultTargets: (KotlinMultiplatformExtension.() -> Unit),
    androidOptions: AndroidOptions?,
    kotlinCompilerOptions: KotlinCompilerOptions,
    contributesCtng: Boolean,
    enableWasmJsTests: Boolean,
    versionPackageName: String?,
    description: String?,
) {
  if (androidOptions != null) {
    configureAndroid(namespace, androidOptions)
  }

  librarianModule(
      group = group(),
      version = version(),
      jvmTarget = jvmTarget ?: 8,
      kotlinTarget = kotlinCompilerOptions.version.version + ".0",
      bcv = Bcv(
          false,
      ) {
        it as AbiValidationVariantSpec
        @OptIn(ExperimentalAbiValidation::class)
        it.filters {
          it.excluded {
            byNames.add("**.internal.**")
          }
        }
      },
      versionPackageName = versionPackageName,
      publishing = if (description != null) {
        Publishing(
            createMissingPublications = true,
            publishPlatformArtifactsInRootModule = false,
            pomMetadata = PomMetadata(
                artifactId = null,
                description = description,
                vcsUrl = githubUrl,
                developer = developer,
                license = license
            ),
            emptyJarLink =  "https://www.apollographql.com/docs/kotlin/kdoc/index.html"
        )
      } else {
        null
      },
      signing = signing(),
  )
  maybeCustomizeDokka()

  if (description != null) {
    extensions.getByType(PublishingExtension::class.java).repositories.apply {
      maven {
        it.name = "pluginTest"
        it.url = uri(rootProject.layout.buildDirectory.dir("localMaven"))
      }
    }
  }

  configureTestAggregationProducer()
  configureJavaAndKotlinCompilers(
      listOf(
          "kotlin.RequiresOptIn",
          "com.apollographql.apollo.annotations.ApolloInternal",
          "com.apollographql.apollo.annotations.ApolloExperimental"
      )
  )

  configurations.configureEach {
    if (it.name == "apolloPublished" || it.name.matches(Regex("apollo.*Compiler"))) {
      // Within the 'tests' project (a composite build), dependencies are automatically substituted to use the project's one.
      // apollo-tooling depends on a published version of apollo-api which should not be substituted for both the runtime
      // and compiler classpaths.
      // See (see https://docs.gradle.org/current/userguide/composite_builds.html#deactivate_included_build_substitutions).
      it.resolutionStrategy.useGlobalDependencySubstitutionRules.set(false)
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
  disableSomeTests(enableWasmJsTests)

  /**
   * `ctng` is short for CiTestNoGradle. It's a shorthand task that runs all the `build`
   * tasks except the Gradle plugin one because it is slow.
   * The name is for historical reasons.
   */
  tasks.register("ctng") {
    if (contributesCtng) {
      it.dependsOn("build")
    }
  }

  tasks.withType(Jar::class.java).configureEach {
    it.manifest {
      it.attributes(mapOf("Automatic-Module-Name" to namespace))
    }
  }

  configureLicensee()
}

internal val Project.kotlinTargets: Collection<KotlinTarget>
  get() {
    return when (val kotlin = extensions.getByName("kotlin")) {
      is KotlinJvmExtension -> listOf(kotlin.target)
      is KotlinMultiplatformExtension -> kotlin.targets
      else -> emptyList()
    }
  }

fun Project.apolloLibrary(
    namespace: String,
    description: String?,
    jvmTarget: Int? = null,
    withJs: Boolean = true,
    withLinux: Boolean = true,
    withApple: Boolean = true,
    withJvm: Boolean = true,
    withWasm: Boolean = true,
    androidOptions: AndroidOptions? = null,
    kotlinCompilerOptions: KotlinCompilerOptions = KotlinCompilerOptions(),
    contributesCtng: Boolean = true,
    enableWasmJsTests: Boolean = true,
    versionPackageName: String? = null,
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
      kotlinCompilerOptions = kotlinCompilerOptions,
      contributesCtng = contributesCtng,
      enableWasmJsTests = enableWasmJsTests,
      versionPackageName = versionPackageName,
      description = description
  )
}

fun Project.apolloTest(
    withJs: Boolean = true,
    withJvm: Boolean = true,
    appleTargets: Set<String> = setOf(hostTarget),
    jvmTarget: Int? = null,
) {
  apolloTest(
      jvmTarget = jvmTarget,
      block = defaultTargets(withJvm = withJvm, withJs = withJs, withLinux = false, withAndroid = false, withWasm = false, appleTargets = appleTargets),
  )
}

fun Project.apolloTest(
    jvmTarget: Int? = null,
    block: KotlinMultiplatformExtension.() -> Unit,
) {
  configureTestAggregationProducer()
  configureJavaCompatibility(jvmTarget ?: 17)
  configureJavaAndKotlinCompilers(
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

fun Project.rootCommon() {
  version = property("VERSION_NAME")!!

  tasks.register("rmbuild") {
    val root = file(".")
    it.doLast {
      root.walk().onEnter {
        if (it.isDirectory && it.name == "build") {
          println("deleting: $it")
          it.deleteRecursively()
          false
        } else {
          true
        }
      }.count()
    }
  }

  configureNode()
  configureTestAggregationConsumer()
}

fun Project.apolloTestRoot() {
  rootCommon()
}

fun Project.apolloLibrariesRoot() {
  rootCommon()

  librarianRoot(
      group = group(),
      version = version(),
      publishing = Publishing(
          createMissingPublications = false,
          publishPlatformArtifactsInRootModule = false,
          pomMetadata = PomMetadata(
              artifactId = "apollo-kdoc",
              description = "Apollo Kotlin documentation",
              vcsUrl = "https://github.com/apollographql/apollo-kotlin/",
              developer = "Apollo",
              license = "MIT",
          ),
          emptyJarLink = null
      ),
      sonatype = Sonatype(
          username = System.getenv("LIBRARIAN_SONATYPE_USERNAME"),
          password = System.getenv("LIBRARIAN_SONATYPE_PASSWORD"),
          validationTimeout = 30.minutes.toJavaDuration(),
          publishingTimeout = 1.hours.toJavaDuration(),
          publishingType = null,
      ),
      signing = signing(),
      gcs = System.getenv("LIBRARIAN_GOOGLE_SERVICES_JSON")?.let{
        Gcs(
            serviceAccountJson = it,
            bucket = "apollo-previews",
            prefix = "m2",
        )
      },
      kdoc = Kdoc(
          includeSelf = false,
          olderVersions = listOf(
              Coordinates("com.apollographql.apollo:apollo-kdoc:4.2.0"),
              Coordinates("com.apollographql.apollo3:apollo-kdoc:3.8.2"),
          )
      )
  )
  maybeCustomizeDokka()
}

