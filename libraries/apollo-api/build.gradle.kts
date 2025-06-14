import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import kotlin.jvm.java

plugins {
  id("org.jetbrains.kotlin.multiplatform")
}

apolloLibrary(
    namespace = "com.apollographql.apollo.api"
)

kotlin {
  sourceSets {
    findByName("commonMain")?.apply {
      dependencies {
        api(libs.okio)
        api(libs.uuid)
        api(project(":apollo-annotations"))
      }
    }
  }
}

abstract class GenerateLibraryVersion : DefaultTask() {
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

    val versionFile = File(outputDir.asFile.get(), "com/apollographql/apollo/api/ApolloApiVersion.kt")
    versionFile.parentFile.mkdirs()
    versionFile.writeText("""// Generated file. Do not edit!
package com.apollographql.apollo.api
const val apolloApiVersion = "${version.get()}"
""")
  }
}

val pluginVersionTaskProvider = tasks.register("generateLibraryVersion", GenerateLibraryVersion::class.java) {
  outputDir.set(project.layout.buildDirectory.dir("generated/kotlin/"))
  version.set(project.version.toString())
}

extensions.getByType(KotlinBaseExtension::class.java).apply {
  val versionFileProvider = pluginVersionTaskProvider.flatMap { it.outputDir }
  sourceSets.getByName("commonMain").kotlin.srcDir(versionFileProvider)
}

tasks.withType(KotlinCompile::class.java) {
  dependsOn(pluginVersionTaskProvider)
}