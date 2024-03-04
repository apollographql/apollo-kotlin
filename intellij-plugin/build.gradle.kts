
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URL
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
    (this as JavaToolchainSpec).languageVersion.set(JavaLanguageVersion.of(properties("javaVersion").toInt()))
  }
}

tasks {
  withType<KotlinCompile> {
    kotlinOptions {
      freeCompilerArgs = listOf("-Xcontext-receivers")
    }
  }

  runIde {
    // Enables debug logging for the plugin
    systemProperty("idea.log.debug.categories", "Apollo")

    // Disable hiding frequent exceptions in logs (annoying for debugging). See com.intellij.idea.IdeaLogger.
    systemProperty("idea.logger.exception.expiration.minutes", "0")

    // Use a custom IntelliJ installation. Set this property in your local ~/.gradle/gradle.properties file.
    // (for AS, it should be something like '/Applications/Android Studio.app/Contents')
    // See https://plugins.jetbrains.com/docs/intellij/android-studio.html#configuring-the-plugin-gradle-build-script
    if (project.hasProperty("apolloIntellijPlugin.ideDir")) {
      localPath.set(file(project.property("apolloIntellijPlugin.ideDir")!!))
    }

    // Uncomment to disable internal mode - see https://plugins.jetbrains.com/docs/intellij/enabling-internal.html
    // systemProperty("idea.is.internal", "false")
  }

  // Log tests
  withType<AbstractTestTask> {
    testLogging {
      exceptionFormat = TestExceptionFormat.FULL
      events.add(TestLogEvent.PASSED)
      events.add(TestLogEvent.FAILED)
      showStandardStreams = true
    }
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
      rtJar.writeBytes(URL("https://github.com/JetBrains/intellij-community/raw/master/java/mockJDK-1.7/jre/lib/rt.jar").openStream().readBytes())
    }
  }
}

tasks.test.configure {
  dependsOn("downloadMockJdk")
  // Setup fake JDK for maven dependencies to work
  // See https://jetbrains-platform.slack.com/archives/CPL5291JP/p1664105522154139 and https://youtrack.jetbrains.com/issue/IJSDK-321
  // Use a relative path to make build caching work
  systemProperty("idea.home.path", mockJdkRoot.relativeTo(project.projectDir).path)
}

apollo {
  service("apolloDebug") {
    packageName.set("com.apollographql.apollo3.debug")
    schemaFiles.from(file("../libraries/apollo-debug-server/src/androidMain/resources/schema.graphqls"))
    introspection {
      endpointUrl.set("http://localhost:12200/")
      schemaFile.set(file("../libraries/apollo-debug-server/src/androidMain/resources/schema.graphqls"))
    }
  }
}

// We're using project(":apollo-gradle-plugin-external") and the published "apollo-runtime" which do not have the same version
tasks.configureEach {
  if (name == "checkApolloVersions") {
    enabled = false
  }
}

// Configure Gradle IntelliJ Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
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

  verifyPlugin {
    ides {
      recommended()
    }
  }
}

dependencies {
  intellijPlatform {
    create(type = properties("platformType"), version = properties("platformVersion"))
    bundledPlugins(properties("platformBundledPlugins").split(',').map(String::trim).filter(String::isNotEmpty))
    plugins(properties("platformPlugins").split(',').map(String::trim).filter(String::isNotEmpty))
    instrumentationTools()
    pluginVerifier()
    testFramework()
  }
  implementation(project(":apollo-gradle-plugin-external"))
  implementation(project(":apollo-ast"))
  implementation(project(":apollo-tooling"))
  implementation(project(":apollo-normalized-cache-sqlite"))
  implementation(libs.sqlite.jdbc)
  implementation(libs.apollo.runtime.published)
  runtimeOnly(libs.slf4j)
  testImplementation(libs.google.testparameterinjector)
}
