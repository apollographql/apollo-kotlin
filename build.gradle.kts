import JapiCmp.configureJapiCmp
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeHostTest

plugins {
  id("build.logic") apply false
}

apply(plugin = "com.github.ben-manes.versions")
apply(plugin = "org.jetbrains.kotlinx.binary-compatibility-validator")

version = property("VERSION_NAME")!!

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

  return eventName == "push" && (ref == "refs/heads/main")
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
  dependsOn(":apollo-gradle-plugin:allTests")
  dependsOn(":apollo-gradle-plugin-external:validatePlugins")
}

tasks.register("ciTestsNoGradle") {
  description = """Execute all tests from the root project except: 
    | - the Gradle ones
    | - most of the Apple tests. Instead it just executes macosX64 tests to save time
    | - the IntelliJ plugin tests - they are run in a dedicated job
  """.trimMargin()


  subprojects {
    if (name !in setOf("apollo-gradle-plugin", "intellij-plugin")) {
      dependsOn(tasks.matching { it.name == "test" })
    }
    dependsOn(tasks.matching { it.name == "jvmTest" })
    dependsOn(tasks.matching { it.name == "jsIrTest" })
    dependsOn(tasks.withType(KotlinNativeHostTest::class.java))
    dependsOn(tasks.matching { it.name == "apiCheck" })
  }

  /**
   * Update the database schemas in CI
   */
  dependsOn(":apollo-normalized-cache-sqlite:generateCommonMainJsonDatabaseSchema")
  dependsOn(":apollo-normalized-cache-sqlite-incubating:generateCommonMainJsonDatabaseSchema")
  dependsOn(":apollo-normalized-cache-sqlite-incubating:generateCommonMainBlobDatabaseSchema")
  dependsOn(":apollo-normalized-cache-sqlite-incubating:generateCommonMainBlob2DatabaseSchema")

  doLast {
    if (isCIBuild()) {
      checkGitStatus()
    }
  }
}

/**
 * Shorthands for less typing during development
 */
tasks.register("ctng") {
  dependsOn("ciTestsNoGradle")
}
tasks.register("ctg") {
  dependsOn("ciTestsGradle")
}

val ciBuild = tasks.register("ciBuild") {
  description = "Execute the 'build' task in each subproject"
  dependsOn(subprojectTasks("build"))
}

rootProject.configureDokka()
tasks.named("dokkaHtmlMultiModule").configure {
  this as org.jetbrains.dokka.gradle.DokkaMultiModuleTask
  outputDirectory.set(layout.buildDirectory.asFile.get().resolve("dokkaHtml/kdoc"))
}

tasks.named("dependencyUpdates").configure {
  (this as com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask)
  rejectVersionIf {
    listOf("alpha", "beta", "rc").any { candidate.version.lowercase().contains(it) }
  }
}

rootProject.configureNode()
rootProject.configureJapiCmp()

configure<kotlinx.validation.ApiValidationExtension> {
  ignoredPackages.addAll(
      listOf(
          /**
           * In general, we rely on annotations or "internal" visibility to hide the non-public APIs. But there are a few exceptions:
           *
           * Gradle plugin: tasks and other classes must be public in order for Gradle to instantiate and decorate them.
           * SQLDelight generated sources are not generated as 'internal'.
           */
          "com.apollographql.apollo3.gradle.internal",
          "com.apollographql.apollo3.cache.normalized.sql.internal",
          "com.apollographql.apollo3.runtime.java.internal",
          "com.apollographql.apollo3.runtime.java.interceptor.internal",
          "com.apollographql.apollo3.runtime.java.network.http.internal",
      )
  )
  ignoredProjects.addAll(
      rootDir.resolve("libraries")
          .listFiles()!!
          .filter { it.resolve("build.gradle.kts").exists() }
          .map { it.name }
          .filter { it.endsWith("-incubating") }
          + "intellij-plugin"
  )
  nonPublicMarkers.addAll(
      listOf(
          "com.apollographql.apollo3.annotations.ApolloInternal",
          "com.apollographql.apollo3.annotations.ApolloExperimental",
      )
  )
}

tasks.register("rmbuild") {
  val root = file(".")
  doLast {
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

rootSetup(ciBuild)