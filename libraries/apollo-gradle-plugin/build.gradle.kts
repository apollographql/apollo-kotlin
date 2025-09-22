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
  codeGeneration {
    addDependencies = false
  }
  pluginMarker("com.apollographql.apollo")
}

val agpCompat = kotlin.target.compilations.create("agp-compat")

mapOf(
    "8" to setOf(libs.android.plugin8),
    "9" to setOf(libs.android.plugin9, libs.kotlin.plugin)
).forEach {
  val compilation = kotlin.target.compilations.create("agp-${it.key}")

  compilation.associateWith(agpCompat)
  kotlin.target.compilations.getByName("main").associateWith(compilation)

  tasks.jar {
    from(compilation.output.classesDirs)
  }
  dependencies {
    add(compilation.compileOnlyConfigurationName, project(":apollo-annotations"))
    it.value.forEach {
      add(compilation.compileOnlyConfigurationName, it)
    }
    // See https://issuetracker.google.com/issues/445209309
    add(compilation.compileOnlyConfigurationName, libs.gradle.api.min)
  }
}

tasks.jar.configure {
  from(agpCompat.output.classesDirs)
}

/**
 * associateWith() pulls the secondary compilations into the main dependencies,
 * which we don't want.
 *
 * An alternative would be to not use `associateWith()` but that fails in the IDE,
 * probably because there is no way to set `AbstractKotlinCompile.friendSourceSets`
 * from public API.
 */
configurations.compileOnly.get().dependencies.removeIf {
  when {
    it is ExternalDependency && it.group == "com.android.tools.build" && it.name == "gradle" -> true
    else -> false
  }
}
/**
 * Also force our own version of KGP
 */
configurations.compileClasspath.get().resolutionStrategy {
  eachDependency {
    val kgp = libs.kgp.compile.get()
    if (requested.group == kgp.group && requested.name == kgp.name) {
      /**
       * Use our declared KGP version
       */
      useVersion(kgp.version!!)
    }
  }
}

kotlin.target.compilations.get("main").apply {
  associateWith(agpCompat)
}

dependencies {
  gratatouille(project(":apollo-gradle-plugin-tasks"))

  add(agpCompat.compileOnlyConfigurationName, libs.gradle.api.min)
  add(agpCompat.compileOnlyConfigurationName, project(":apollo-annotations"))

  compileOnly(libs.gradle.api.min)
  compileOnly(libs.kgp.compile)

  implementation(project(":apollo-annotations"))
  implementation(libs.gratatouille.wiring.runtime)

  testImplementation(project(":apollo-ast"))
  testImplementation(libs.junit)
  testImplementation(libs.truth)
  testImplementation(libs.assertj)
  testImplementation(libs.okhttp.mockwebserver)
  testImplementation(libs.okhttp.tls)
  testImplementation(libs.apollo.execution)
  testImplementation(libs.apollo.execution.http4k)
  testImplementation(gradleTestKit())
  testImplementation(platform(libs.http4k.bom))
  testImplementation(libs.http4k.core)
  testImplementation(libs.http4k.server.jetty)
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
