import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  antlr
  kotlin("jvm")
  kotlin("kapt")
}

dependencies {
  antlr(groovy.util.Eval.x(project, "x.dep.antlr.antlr"))
  implementation(groovy.util.Eval.x(project, "x.dep.moshi.adapters"))
  implementation(groovy.util.Eval.x(project, "x.dep.moshi.moshi"))
  implementation(groovy.util.Eval.x(project, "x.dep.poet.kotlin"))
  implementation(project(":apollo-api"))

  kapt(groovy.util.Eval.x(project, "x.dep.moshi.kotlinCodegen"))

  testImplementation(groovy.util.Eval.x(project, "x.dep.compiletesting"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.kotlinCompileTesting"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.junit"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.truth"))
}

abstract class GeneratePluginVersion : DefaultTask() {
  @get:org.gradle.api.tasks.Input
  abstract val version: Property<String>

  @get:org.gradle.api.tasks.OutputDirectory
  abstract val outputDir: DirectoryProperty

  @org.gradle.api.tasks.TaskAction
  fun taskAction() {
    val versionFile = File(outputDir.asFile.get(), "Version.kt")
    versionFile.parentFile.mkdirs()
    versionFile.writeText("""// Generated file. Do not edit!
package com.apollographql.apollo.compiler
val VERSION = "${project.version}"
""")
  }
}

val pluginVersionTaskProvider = tasks.register("pluginVersion", GeneratePluginVersion::class.java) {
  outputDir.set(project.layout.buildDirectory.dir("generated/kotlin/com/apollographql/apollo/compiler/"))
  version.set(project.version.toString())
}

configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
  val versionFileProvider = pluginVersionTaskProvider.flatMap { it.outputDir }
  sourceSets.getByName("main").kotlin.srcDir(versionFileProvider)
}


// since test/graphql is not an input to Test tasks, they're not run with the changes made in there.
tasks.withType<Test>().configureEach {
  inputs.dir("src/test/graphql")
}
