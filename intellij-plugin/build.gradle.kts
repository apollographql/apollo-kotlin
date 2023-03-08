import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URL

fun properties(key: String) = project.findProperty(key).toString()

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("org.jetbrains.intellij")
  id("org.jetbrains.changelog")
  id("maven-publish")
}

// XXX: this should use the settings repositories instead
repositories {
  // Uncomment this one to use the Kotlin "dev" repository
  // maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev/") }
  mavenCentral()
}

group = properties("pluginGroup")

// Use the global version defined in the root project + snapshot suffix if from the CI
version = properties("VERSION_NAME") + if (System.getenv("COM_APOLLOGRAPHQL_IJ_PLUGIN_SNAPSHOT").toBoolean()) ".${properties("snapshotVersion")}" else ""

// Set the JVM language level used to build project. Use Java 11 for 2020.3+, and Java 17 for 2022.2+.
kotlin {
  jvmToolchain {
    (this as JavaToolchainSpec).languageVersion.set(JavaLanguageVersion.of(properties("javaVersion").toInt()))
  }
}

// Configure Gradle IntelliJ Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
  pluginName.set(properties("pluginName"))
  version.set(properties("platformVersion"))
  type.set(properties("platformType"))

  // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file.
  plugins.set(properties("platformPlugins").split(',').map(String::trim).filter(String::isNotEmpty))
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
  version.set(project.version.toString())
  groups.set(emptyList())
}

tasks {
  withType<KotlinCompile> {
    kotlinOptions {
      freeCompilerArgs = listOf("-Xcontext-receivers")
    }
  }

  patchPluginXml {
    pluginId.set(properties("pluginId"))
    version.set(project.version.toString())
    sinceBuild.set(properties("pluginSinceBuild"))
    untilBuild.set(properties("pluginUntilBuild"))

    // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
    pluginDescription.set(
        projectDir.resolve("README.md").readText().lines().run {
          val start = "<!-- Plugin description -->"
          val end = "<!-- Plugin description end -->"

          if (!containsAll(listOf(start, end))) {
            throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
          }
          subList(indexOf(start) + 1, indexOf(end))
        }.joinToString("\n").run { markdownToHTML(this) }
    )

    // Get the latest available change notes from the changelog file
    changeNotes.set(provider {
      changelog.run {
        getOrNull(project.version.toString()) ?: getUnreleased()
      }.toHTML()
    })
  }

  // Configure UI tests plugin
  // Read more: https://github.com/JetBrains/intellij-ui-test-robot
  runIdeForUiTests {
    systemProperty("robot-server.port", "8082")
    systemProperty("ide.mac.message.dialogs.as.sheets", "false")
    systemProperty("jb.privacy.policy.text", "<!--999.999-->")
    systemProperty("jb.consents.confirmation.enabled", "false")

    // Enables debug logging for the plugin
    systemProperty("idea.log.debug.categories", "Apollo")
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
      ideDir.set(file(project.property("apolloIntellijPlugin.ideDir")!!))
    }
  }

  signPlugin {
    certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
    privateKey.set(System.getenv("PRIVATE_KEY"))
    password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
  }

  publishPlugin {
    dependsOn("patchChangelog")
    token.set(System.getenv("PUBLISH_TOKEN"))
    // Currently we release to a specific "preview" release channel so the plugin is not listed on the Marketplace
    // Change to "default" to release to the main channel.
    // Read more: https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
    channels.set(listOf("preview"))
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

  test {
    // Setup fake JDK for maven dependencies to work
    // See https://jetbrains-platform.slack.com/archives/CPL5291JP/p1664105522154139 and https://youtrack.jetbrains.com/issue/IJSDK-321
    systemProperty("idea.home.path", file("mockJDK").absolutePath)
  }
}

// Setup fake JDK for maven dependencies to work
// See https://jetbrains-platform.slack.com/archives/CPL5291JP/p1664105522154139 and https://youtrack.jetbrains.com/issue/IJSDK-321
tasks.register("downloadMockJdk") {
  doLast {
    val rtJar = file("mockJDK/java/mockJDK-1.7/jre/lib/rt.jar")
    if (!rtJar.exists()) {
      rtJar.parentFile.mkdirs()
      rtJar.writeBytes(URL("https://github.com/JetBrains/intellij-community/raw/master/java/mockJDK-1.7/jre/lib/rt.jar").openStream().readBytes())
    }
  }
}

tasks.named("test").configure {
  dependsOn("downloadMockJdk")
}

// See https://plugins.jetbrains.com/docs/intellij/custom-plugin-repository.html
tasks.register("updatePluginsXml") {
  val filePath = "snapshots/plugins.xml"
  val pluginId = properties("pluginId")
  val pluginName = properties("pluginName")
  val version = project.version.toString()
  val pluginSinceBuild = properties("pluginSinceBuild")
  val pluginUntilBuild = properties("pluginUntilBuild")
  inputs.property("pluginId", pluginId)
  inputs.property("pluginName", pluginName)
  inputs.property("version", version)
  inputs.property("pluginSinceBuild", pluginSinceBuild)
  inputs.property("pluginUntilBuild", pluginUntilBuild)
  outputs.file(filePath)
  outputs.cacheIf { true }
  doLast {
    file(filePath).writeText(
        """
        <plugins>
          <plugin
              id="$pluginId"
              url="https://repsy.io/mvn/bod/apollo-intellij-plugin/com/apollographql/$pluginName/$version/$pluginName-$version.zip"
              version="$version">
            <idea-version since-build="$pluginSinceBuild" until-build="$pluginUntilBuild"/>
            <name>Apollo GraphQL (Snapshot)</name>
          </plugin>
        </plugins>
        """.trimIndent()
    )
  }
}

publishing {
  repositories {
    maven {
      name = "repsyIjPluginSnapshots"
      url = uri("https://repo.repsy.io/mvn/bod/apollo-intellij-plugin/")
      credentials {
        username = System.getenv("IJ_PLUGIN_REPSY_USERNAME")
        password = System.getenv("IJ_PLUGIN_REPSY_PASSWORD")
      }
    }
  }

  publications {
    create<MavenPublication>("default") {
      artifactId = properties("pluginName")
      artifact(tasks.named("buildPlugin"))
    }
  }
}

dependencies {
  implementation(project(":apollo-gradle-plugin-external"))
}
