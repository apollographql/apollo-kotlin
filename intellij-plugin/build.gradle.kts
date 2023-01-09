import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URL

fun properties(key: String) = project.findProperty(key).toString()

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("org.jetbrains.intellij").version("1.8.0")
  id("org.jetbrains.changelog").version("1.3.1")
}

group = properties("pluginGroup")
version = properties("pluginVersion")

repositories {
  mavenCentral()
}

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
  version.set(properties("pluginVersion"))
  groups.set(emptyList())
}

tasks {
  withType<KotlinCompile> {
    kotlinOptions {
      freeCompilerArgs = listOf("-Xcontext-receivers")
    }
  }

  patchPluginXml {
    version.set(properties("pluginVersion"))
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
        getOrNull(properties("pluginVersion")) ?: getUnreleased()
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
    // pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
    // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
    // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
    channels.set(listOf(properties("pluginVersion").split('-').getOrElse(1) { "default" }.split('.').first()))
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
