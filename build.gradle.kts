import kotlinx.validation.ExperimentalBCVApi
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeHostTest

plugins {
  id("base")
}

buildscript {
  dependencies {
    classpath("com.apollographql.apollo:build-logic")
  }
}

apply(plugin = "com.github.ben-manes.versions")
apply(plugin = "org.jetbrains.kotlinx.binary-compatibility-validator")

version = property("VERSION_NAME")!!

fun subprojectTasks(name: String): List<Task> {
  return subprojects.flatMap { subproject ->
    subproject.tasks.matching { it.name == name }
  }
}

tasks.register("ciTestsGradle") {
  description = "Execute the Gradle tests (slow)"
  dependsOn(":apollo-gradle-plugin:test")
}

tasks.register("ciTestsNoGradle") {
  description = """Execute all tests from the root project except: 
    | - the Gradle ones
    | - most of the Apple tests. Instead it just executes macosX64 tests to save time
    | - the IntelliJ plugin tests - they are run in a dedicated job
  """.trimMargin()


  subprojects {
    if (name !in setOf("apollo-gradle-plugin")) {
      dependsOn(tasks.matching { it.name == "test" })
    }
    dependsOn(tasks.matching { it.name == "jvmTest" })
    dependsOn(tasks.matching { it.name == "jsNodeTest" })
    dependsOn(tasks.withType(KotlinNativeHostTest::class.java))
    dependsOn(tasks.matching { it.name == "apiCheck" })
    dependsOn(tasks.matching { it.name == "licensee" })
  }

  /**
   * Update the database schemas in CI
   */
  dependsOn(":apollo-normalized-cache-sqlite:generateCommonMainJsonDatabaseSchema")

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

tasks.named("dependencyUpdates").configure {
  (this as com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask)
  rejectVersionIf {
    listOf("alpha", "beta", "rc").any { candidate.version.lowercase().contains(it) }
  }
}

configure<kotlinx.validation.ApiValidationExtension> {
  @OptIn(ExperimentalBCVApi::class)
  klib.enabled = true

  ignoredPackages.addAll(
      listOf(
          /**
           * In general, we rely on annotations or "internal" visibility to hide the non-public APIs. But there are a few exceptions:
           *
           * Gradle plugin: tasks and other classes must be public in order for Gradle to instantiate and decorate them.
           * SQLDelight generated sources are not generated as 'internal'.
           */
          "com.apollographql.apollo.gradle.internal",
          "com.apollographql.apollo.cache.normalized.sql.internal",
          "com.apollographql.apollo.runtime.java.internal",
          "com.apollographql.apollo.runtime.java.interceptor.internal",
          "com.apollographql.apollo.runtime.java.network.http.internal",
      )
  )
  ignoredProjects.add("apollo-testing-support-internal")
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

apolloRoot()
