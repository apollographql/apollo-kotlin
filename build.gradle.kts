import JapiCmp.configureJapiCmp
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

buildscript {
  repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
  }
  project.apply {
    from(rootProject.file("gradle/dependencies.gradle"))
  }
  dependencies {
    classpath("com.apollographql.apollo:build-logic")
  }
}

apply(plugin = "com.github.ben-manes.versions")
apply(plugin = "org.jetbrains.dokka")
apply(plugin = "org.jetbrains.kotlinx.binary-compatibility-validator")


version = property("VERSION_NAME")!!

subprojects {
  apply {
    from(rootProject.file("gradle/dependencies.gradle"))
  }

  configureJavaAndKotlinCompilers()

  tasks.withType<Test> {
    systemProperty("updateTestFixtures", System.getProperty("updateTestFixtures"))
    systemProperty("testFilter", System.getProperty("testFilter"))
    systemProperty("codegenModels", System.getProperty("codegenModels"))
  }
  tasks.withType<AbstractTestTask> {
    testLogging {
      exceptionFormat = TestExceptionFormat.FULL
    }
  }

  repositories {
    google()
    mavenCentral()
    jcenter {
      content {
        // https://github.com/Kotlin/kotlinx-nodejs/issues/16
        includeModule("org.jetbrains.kotlinx", "kotlinx-nodejs")
      }
    }
  }

  group = property("GROUP")!!
  version = property("VERSION_NAME")!!

  configurePublishing()

  /**
   * Type `echo "apollographql_android_hack=true\n" >> ~/.gradle/gradle.properties` on your development machine
   * to make MPP modules publish an Android artifact so that IntelliJ can resolve the symbols
   *
   * See https://youtrack.jetbrains.com/issue/KTIJ-14471
   */
  if (properties["apollographql_android_hack"] == "true") {
    pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
      // Hack for autocomplete to work with android projects
      // See https://youtrack.jetbrains.com/issue/KTIJ-14471
      if (System.getProperty("idea.sync.active") != null) {
        apply(plugin = "com.android.library")
        (extensions.findByName("kotlin") as? org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension)?.apply {
          android()
        }
      }
    }
  }
}

fun subprojectTasks(name: String): List<Task> {
  return subprojects.flatMap { subproject ->
    subproject.tasks.matching { it.name == name }
  }
}

fun isTag(): Boolean {
  val ref = System.getenv("GITHUB_REF")

  return ref?.startsWith("refs/tags/") == true
}

fun shouldPublishSnapshots(): Boolean {
  val eventName = System.getenv("GITHUB_EVENT_NAME")
  val ref = System.getenv("GITHUB_REF")

  return eventName == "push" && (ref == "refs/heads/main" || ref == "refs/heads/dev-3.x")
}

tasks.register("ciPublishSnapshot") {
  description = "Publishes a SNAPSHOT"

  if (shouldPublishSnapshots()) {
    dependsOn(subprojectTasks("publishAllPublicationsToOssSnapshotsRepository"))
  } else {
    doFirst {
      error("We are not on a branch, fail snapshots publishing")
    }
  }
}


tasks.register("ciPublishRelease") {
  description = "Publishes all artifacts to OSSRH and the Gradle Plugin Portal"

  if (isTag()) {
    dependsOn(subprojectTasks("publishAllPublicationsToOssStagingRepository"))
    // Only publish plugins to the Gradle portal if everything else succeeded
    finalizedBy(":apollo-gradle-plugin:publishPlugins")
  } else {
    doFirst {
      error("We are not on a tag, fail release publishing")
    }
  }

}

tasks.register("ciTestsGradle") {
  description = "Execute the Gradle tests (slow)"
  dependsOn(":apollo-gradle-plugin:test")
}

tasks.register("ciTestsNoGradle") {
  description = """Execute all tests from the root project except: 
    | - the Gradle ones
    | - most of the apple tests where it executes only macosX64
  """.trimMargin()
  subprojects {
    tasks.configureEach {
      when (name) {
        "test" -> {
          if (this@subprojects.name != "apollo-gradle-plugin") {
            this@register.dependsOn(this)
          }
        }
        "jvmTest", "jsIrTest", "macosX64Test", "apiCheck" -> {
          this@register.dependsOn(this)
        }
      }
    }
  }
}

tasks.register("ciFull") {
  description = "Execute the 'build' task in each subproject"
  dependsOn(subprojectTasks("build"))
}

repositories {
  mavenCentral() // for dokka
}

tasks.named("dokkaHtmlMultiModule").configure {
  this as org.jetbrains.dokka.gradle.DokkaMultiModuleTask
  outputDirectory.set(buildDir.resolve("dokkaHtml/kdoc"))
}

tasks.named("dependencyUpdates").configure {
  (this as com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask)
  rejectVersionIf {
    listOf("alpha", "beta", "rc").any { candidate.version.toLowerCase().contains(it) }
  }
}


tasks.withType(org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask::class.java).configureEach {
      args.addAll(
          listOf(
              "--network-concurrency",
              "1",
              "--mutex",
              "network"
          )
      )
    }
// See https://youtrack.jetbrains.com/issue/KT-47215
plugins.withType<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin> {
  the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().disableGranularWorkspaces()
}

rootProject.configureJapiCmp()

configure<kotlinx.validation.ApiValidationExtension> {
  ignoredPackages.addAll(
      listOf(
          "com.apollographql.apollo3.api.internal",
          "com.apollographql.apollo3.gradle.internal",
          "com.apollographql.apollo3.cache.normalized.internal",
      )
  )
  ignoredProjects.addAll(
      listOf(
          "apollo-compiler",
          "apollo-ast",
          "apollo-testing-support"
      )
  )
  nonPublicMarkers.addAll(
      listOf(
          "com.apollographql.apollo3.api.ApolloInternal",
          "com.apollographql.apollo3.api.ApolloExperimental",
      )
  )
}
