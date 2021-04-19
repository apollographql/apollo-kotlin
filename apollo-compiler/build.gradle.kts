import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  antlr
  kotlin("jvm")
  id("com.google.devtools.ksp")
}

dependencies {
  antlr(groovy.util.Eval.x(project, "x.dep.antlr.antlr"))
  implementation(groovy.util.Eval.x(project, "x.dep.antlr.runtime"))
  implementation(groovy.util.Eval.x(project, "x.dep.moshi.adapters"))
  implementation(groovy.util.Eval.x(project, "x.dep.moshi.moshi"))
  implementation(groovy.util.Eval.x(project, "x.dep.poet.kotlin"))
  implementation(project(":apollo-api"))

  ksp(groovy.util.Eval.x(project, "x.dep.moshi.kotlinCodegen"))

  testImplementation(groovy.util.Eval.x(project, "x.dep.compiletesting"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.kotlinCompileTesting"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.junit"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.truth"))
}

// Only expose the antlr runtime dependency
// See https://github.com/gradle/gradle/issues/820#issuecomment-288838412
configurations[JavaPlugin.API_CONFIGURATION_NAME].let { apiConfiguration ->
  apiConfiguration.setExtendsFrom(apiConfiguration.extendsFrom.filter { it.name != "antlr" })
}

abstract class GeneratePluginVersion : DefaultTask() {
  @get:org.gradle.api.tasks.Input
  abstract val version: Property<String>

  @get:org.gradle.api.tasks.OutputDirectory
  abstract val outputDir: DirectoryProperty

  @org.gradle.api.tasks.TaskAction
  fun taskAction() {
    val versionFile = File(outputDir.asFile.get(), "com/apollographql/apollo3/compiler/Version.kt")
    versionFile.parentFile.mkdirs()
    versionFile.writeText("""// Generated file. Do not edit!
package com.apollographql.apollo3.compiler
val VERSION = "${project.version}"
""")
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
  // Fixes the warning below:
  // "Task ':apollo-android:apollo-compiler:kaptGenerateStubsKotlin' uses the output of task ':apollo-android:apollo-compiler:pluginVersion', without declaring an explicit dependency"
  dependsOn(pluginVersionTaskProvider)
  // This used to work and fails now. Strangely enough, it fails on both `dev-3.x` and `main` as of writing while both these branches have
  // compiled successfully before...
  dependsOn("generateGrammarSource")
}

tasks.withType(KotlinCompile::class.java) {
  kotlinOptions {
    // Gradle forces 1.3.72 for the time being so compile against 1.3 stdlib for the time being
    // See https://issuetracker.google.com/issues/166582569
    apiVersion = "1.3"
  }
}

// since test/graphql is not an input to Test tasks, they're not run with the changes made in there.
tasks.withType<Test>().configureEach {
  inputs.dir("src/test/graphql")
  inputs.dir("src/test/sdl")
  inputs.dir("src/test/typename")
  inputs.dir("src/test/usedtypes")
  inputs.dir("src/test/validation")
}
