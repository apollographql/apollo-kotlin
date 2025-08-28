import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.google.devtools.ksp")
  id("com.gradleup.gratatouille.wiring")
  id("com.android.lint")
}

apolloLibrary(
    namespace = "com.apollographql.apollo.gradle",
    jvmTarget = 11, // To compile against AGP 8.0.0
    kotlinCompilerOptions = KotlinCompilerOptions(KotlinVersion.KOTLIN_1_9) // For better Gradle compatibility
)

gratatouille {
  codeGeneration()
  pluginMarker("com.apollographql.apollo")
}

dependencies {
  gratatouille(project(":apollo-gradle-plugin-tasks"))

  compileOnly(libs.gradle.api.min)
  compileOnly(libs.kotlin.plugin.min)
  compileOnly(libs.android.plugin.min)

  compileOnly(libs.gradle.api.min)
  implementation(project(":apollo-annotations"))
  testImplementation(project(":apollo-ast"))
  testImplementation(libs.junit)
  testImplementation(libs.truth)
  testImplementation(libs.assertj)
  testImplementation(libs.okhttp.mockwebserver)
  testImplementation(libs.okhttp.tls)
  testImplementation(libs.apollo.execution)
  testImplementation(libs.apollo.execution.http4k)
  testImplementation(gradleTestKit())

  testImplementation(platform(libs.http4k.bom.get()))
  testImplementation(libs.http4k.core)
  testImplementation(libs.http4k.server.jetty)
  testImplementation(libs.slf4j.nop.get().toString()) {
    because("jetty uses SL4F")
  }
  lintChecks(libs.androidx.lint.rules)
}

/**
 * A task that removes the stale test projects from the build directory.
 * We don't want to remove them at the end of the build because it's sometimes useful to have
 * the project stick around for inspection/debugging.
 *
 * Instead, this task is run as a prerequisite to the tests. It is never up to date and will
 * always scan the contents of the `build` directory.
 */
abstract class CleanStaleTestProjects : DefaultTask() {
  @get:Internal
  abstract var directory: File

  @TaskAction
  fun taskAction() {
    directory.listFiles { it: File -> it.isDirectory && it.name.startsWith("testProject") }!!.forEach {
      it.deleteRecursively()
    }
  }
}

val cleanStaleTestProjects = tasks.register("cleanStaleTestProjects", CleanStaleTestProjects::class.java) {
  directory = layout.buildDirectory.asFile.get()
}

val publishDependencies = tasks.register("publishDependencies") {
  dependsOn("publishAllPublicationsToPluginTestRepository")
  dependsOn(":apollo-annotations:publishAllPublicationsToPluginTestRepository")
  dependsOn(":apollo-api:publishAllPublicationsToPluginTestRepository")
  dependsOn(":apollo-ast:publishAllPublicationsToPluginTestRepository")
  dependsOn(":apollo-normalized-cache-api:publishAllPublicationsToPluginTestRepository")
  dependsOn(":apollo-mpp-utils:publishAllPublicationsToPluginTestRepository")
  dependsOn(":apollo-compiler:publishAllPublicationsToPluginTestRepository")
  dependsOn(":apollo-gradle-plugin-tasks:publishAllPublicationsToPluginTestRepository")
  dependsOn(":apollo-tooling:publishAllPublicationsToPluginTestRepository")
}

tasks.withType<Test> {
  dependsOn(publishDependencies)
  dependsOn(cleanStaleTestProjects)

  addRelativeInput("testFiles", "testFiles")
  addRelativeInput("testProjects", "testProjects")

  maxHeapSize = "1g"

//  debug = true
  maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1

  val javaToolchains = project.extensions.getByName("javaToolchains") as JavaToolchainService
  javaLauncher.set(javaToolchains.launcherFor {
    // Run all tests using java 17
    languageVersion.set(JavaLanguageVersion.of(17))
  })
}

tasks.register("acceptAndroidLicenses") {
  val source = rootProject.file("android-licenses/android-sdk-preview-license")
  val target = rootProject.file("${System.getenv("ANDROID_HOME")}/licenses/android-sdk-preview-license")
  doLast {
    source.copyTo(target, overwrite = true)
  }
}

tasks.named("test").configure {
  dependsOn("acceptAndroidLicenses")
}

abstract class GeneratePluginVersion : DefaultTask() {
  @get:Input
  abstract val version: Property<String>

  @get:OutputDirectory
  abstract val outputDir: DirectoryProperty

  @TaskAction
  fun taskAction() {
    outputDir.asFile.get().apply {
      deleteRecursively()
      mkdirs()
    }

    val versionFile = File(outputDir.asFile.get(), "com/apollographql/apollo/gradle/Version.kt")
    versionFile.parentFile.mkdirs()
    versionFile.writeText("""// Generated file. Do not edit!
package com.apollographql.apollo.gradle
const val APOLLO_VERSION = "${version.get()}"
"""
    )
  }
}

val pluginVersionTaskProvider = tasks.register("pluginVersion", GeneratePluginVersion::class.java) {
  outputDir.set(project.layout.buildDirectory.dir("generated/kotlin/"))
  version.set(project.version.toString())
}

configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
  val versionFileProvider = pluginVersionTaskProvider.flatMap { it.outputDir }
  sourceSets.getByName("main").kotlin.srcDir(versionFileProvider)
}

tasks.withType(KotlinCompile::class.java) {
  dependsOn(pluginVersionTaskProvider)
}
