import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel.COMPATIBILITY_PROBLEMS
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel.INTERNAL_API_USAGES
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel.INVALID_PLUGIN
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel.PLUGIN_STRUCTURE_WARNINGS
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date

fun properties(key: String) = project.findProperty(key).toString()

fun isSnapshotBuild() = System.getenv("IJ_PLUGIN_SNAPSHOT").toBoolean()


plugins {
  id("org.jetbrains.kotlin.jvm")
  id("org.jetbrains.intellij.platform")
  alias(libs.plugins.apollo.published)
}

commonSetup()

// XXX: this should use the settings repositories instead
repositories {
  // Uncomment this one to use the Kotlin "dev" repository
  // maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev/") }
  mavenCentral()

  intellijPlatform {
    defaultRepositories()
  }
}

group = properties("pluginGroup")

// Use the global version defined in the root project + dedicated suffix if building a snapshot from the CI
version = properties("VERSION_NAME") + getSnapshotVersionSuffix()

fun getSnapshotVersionSuffix(): String {
  if (!isSnapshotBuild()) return ""
  return ".${SimpleDateFormat("YYYY-MM-dd").format(Date())}." + System.getenv("GITHUB_SHA").take(7)
}

// Set the JVM language level used to build project. Use Java 11 for 2020.3+, and Java 17 for 2022.2+.
kotlin {
  jvmToolchain {
    languageVersion = JavaLanguageVersion.of(21)
  }
}

val apolloDependencies = configurations.create("apolloDependencies").apply {
  listOf(":apollo-annotations", ":apollo-api", ":apollo-runtime").forEach {
    dependencies.add(project.dependencies.project(it, "jvmApiElements"))
  }
}

tasks {
  val runLocalIde by intellijPlatformTesting.runIde.registering {
    // Use a custom IJ/AS installation. Set this property in your local ~/.gradle/gradle.properties file.
    // (for AS, it should be something like '/Applications/Android Studio.app/Contents')
    // See https://plugins.jetbrains.com/docs/intellij/android-studio.html#configuring-the-plugin-gradle-build-script
    providers.gradleProperty("apolloIntellijPlugin.ideDir").orNull?.let {
      localPath.set(file(it))
    }

    task {
      // Enables debug logging for the plugin
      systemProperty("idea.log.debug.categories", "Apollo")

      // Disable hiding frequent exceptions in logs (annoying for debugging). See com.intellij.idea.IdeaLogger.
      systemProperty("idea.logger.exception.expiration.minutes", "0")

      // Uncomment to disable internal mode - see https://plugins.jetbrains.com/docs/intellij/enabling-internal.html
      // systemProperty("idea.is.internal", "false")

      // Enable K2 mode (can't be done in the UI in sandbox mode - see https://kotlin.github.io/analysis-api/testing-in-k2-locally.html)
      systemProperty("idea.kotlin.plugin.use.k2", "true")
    }
  }

  // Log tests
  withType<AbstractTestTask> {
    testLogging {
      exceptionFormat = TestExceptionFormat.FULL
      events.add(TestLogEvent.PASSED)
      events.add(TestLogEvent.FAILED)
      showStandardStreams = true
    }
    inputs.files(apolloDependencies)
  }
}

val mockJdkRoot = layout.buildDirectory.asFile.get().resolve("mockJDK")

// Setup fake JDK for maven dependencies to work
// See https://jetbrains-platform.slack.com/archives/CPL5291JP/p1664105522154139 and https://youtrack.jetbrains.com/issue/IJSDK-321
tasks.register("downloadMockJdk") {
  val mockJdkRoot = mockJdkRoot
  doLast {
    val rtJar = mockJdkRoot.resolve("java/mockJDK-1.7/jre/lib/rt.jar")
    if (!rtJar.exists()) {
      rtJar.parentFile.mkdirs()
      rtJar.writeBytes(URI("https://github.com/JetBrains/intellij-community/raw/master/java/mockJDK-1.7/jre/lib/rt.jar").toURL()
          .openStream()
          .readBytes()
      )
    }
  }
}

tasks.test.configure {
  dependsOn("downloadMockJdk")
  // Setup fake JDK for maven dependencies to work
  // See https://jetbrains-platform.slack.com/archives/CPL5291JP/p1664105522154139 and https://youtrack.jetbrains.com/issue/IJSDK-321
  // Use a relative path to make build caching work
  systemProperty("idea.home.path", mockJdkRoot.relativeTo(project.projectDir).path)

  // Enable K2 mode - see https://kotlin.github.io/analysis-api/testing-in-k2-locally.html
  systemProperty("idea.kotlin.plugin.use.k2", "true")
}

apollo {
  service("apolloDebugServer") {
    packageName.set("com.apollographql.ijplugin.apollodebugserver")
    schemaFiles.from(file("../libraries/apollo-debug-server/graphql/schema.graphqls"))
    introspection {
      endpointUrl.set("http://localhost:12200/")
      schemaFile.set(file("../libraries/apollo-debug-server/graphql/schema.graphqls"))
    }
  }
}

// We're using project(":apollo-gradle-plugin-external") and the published "apollo-runtime" which do not have the same version
tasks.configureEach {
  if (name == "checkApolloVersions") {
    enabled = false
  }
}

val apolloPublished = configurations.dependencyScope("apolloPublished").get()

configurations.getByName("implementation").extendsFrom(apolloPublished)

dependencies {
  // IntelliJ Platform dependencies must be declared before the intellijPlatform block - see https://github.com/JetBrains/intellij-platform-gradle-plugin/issues/1784
  intellijPlatform {
    create(type = properties("platformType"), version = properties("platformVersion"))
    bundledPlugins(properties("platformBundledPlugins").split(',').map(String::trim).filter(String::isNotEmpty))
    plugins(properties("platformPlugins").split(',').map(String::trim).filter(String::isNotEmpty))
    instrumentationTools()
    pluginVerifier()
    testFramework(TestFrameworkType.Plugin.Java)
    zipSigner()
  }

  // Coroutines must be excluded to avoid a conflict with the version bundled with the IDE
  // See https://plugins.jetbrains.com/docs/intellij/using-kotlin.html#coroutinesLibraries
  implementation(project(":apollo-gradle-plugin-external")) {
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
  }
  implementation(project(":apollo-ast"))
  implementation(project(":apollo-tooling")) {
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
  }
  implementation(project(":apollo-normalized-cache-sqlite"))
  implementation(libs.sqlite.jdbc)
  implementation(libs.apollo.normalizedcache.sqlite.incubating) {
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
  }
  add("apolloPublished", libs.apollo.runtime.published.get().toString()) {
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
  }
  runtimeOnly(libs.slf4j.simple)
  testImplementation(libs.google.testparameterinjector)

  // Temporary workaround for https://github.com/JetBrains/intellij-platform-gradle-plugin/issues/1663
  // Should be fixed in platformVersion 2024.3.x
  testRuntimeOnly("org.opentest4j:opentest4j:1.3.0")
}

// IntelliJ Platform Gradle Plugin configuration
// See https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-extension.html#intellijPlatform-pluginConfiguration
intellijPlatform {
  pluginConfiguration {
    id.set(properties("pluginId"))
    name.set(properties("pluginName"))
    version.set(project.version.toString())
    ideaVersion {
      sinceBuild = properties("pluginSinceBuild")
      untilBuild = properties("pluginUntilBuild")
    }
    // Extract the <!-- Plugin description --> section from README.md and provide it to the plugin's manifest
    description.set(
        projectDir.resolve("README.md").readText().lines().run {
          val start = "<!-- Plugin description -->"
          val end = "<!-- Plugin description end -->"

          if (!containsAll(listOf(start, end))) {
            throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
          }
          subList(indexOf(start) + 1, indexOf(end))
        }.joinToString("\n").run { markdownToHTML(this) }
    )
    changeNotes.set(
        if (isSnapshotBuild()) {
          "Weekly snapshot builds contain the latest changes from the <code>main</code> branch."
        } else {
          "See the <a href=\"https://github.com/apollographql/apollo-kotlin/releases/tag/v${project.version}\">release notes</a>."
        }
    )
  }

  signing {
    certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
    privateKey.set(System.getenv("PRIVATE_KEY"))
    password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
  }

  publishing {
    token.set(System.getenv("PUBLISH_TOKEN"))
    if (isSnapshotBuild()) {
      // Read more: https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html#specifying-a-release-channel
      channels.set(listOf("snapshots"))
    }
  }

  pluginVerification {
    ides {
      recommended()
    }
    failureLevel.set(
        setOf(
            COMPATIBILITY_PROBLEMS,
            INTERNAL_API_USAGES,
            INVALID_PLUGIN,
            PLUGIN_STRUCTURE_WARNINGS,
        )
    )
  }
}
