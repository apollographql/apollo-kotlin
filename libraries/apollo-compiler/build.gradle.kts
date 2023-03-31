import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.library")
  id("org.jetbrains.kotlin.plugin.serialization")
}

apolloLibrary {
  javaModuleName("com.apollographql.apollo3.compiler")
}

dependencies {
  implementation(project(":apollo-ast"))
  api(golatac.lib("poet.kotlin")) {
    // We don't use any of the KotlinPoet kotlin-reflect features
    exclude(module = "kotlin-reflect")
  }
  api(golatac.lib("poet.java"))

  implementation(golatac.lib("kotlinx.serialization.json"))
  implementation(golatac.lib("kotlinx.serialization.json.okio"))

  testImplementation(golatac.lib("kotlin.compiletesting"))
  testImplementation(golatac.lib("google.testing.compile"))
  testImplementation(golatac.lib("truth"))
  testImplementation(golatac.lib("kotlin.test.junit"))
  testImplementation(golatac.lib("google.testparameterinjector"))
  testImplementation(project(":apollo-api-java")) {
    because("Generated Java code references Java and Guava Optionals")
  }
  testImplementation(golatac.lib("androidx.annotation")) {
    because("Used in the Java generated code")
  }
}

abstract class GeneratePluginVersion : DefaultTask() {
  @get:Input
  abstract val version: Property<String>

  @get:OutputDirectory
  abstract val outputDir: DirectoryProperty

  @TaskAction
  fun taskAction() {
    val versionFile = File(outputDir.asFile.get(), "com/apollographql/apollo3/compiler/Version.kt")
    versionFile.parentFile.mkdirs()
    versionFile.writeText("""// Generated file. Do not edit!
package com.apollographql.apollo3.compiler
const val APOLLO_VERSION = "${version.get()}"
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
}

// since test/graphql is not an input to Test tasks, they're not run with the changes made in there.
tasks.withType<Test>().configureEach {
  inputs.dir("src/test/graphql")
  inputs.dir("src/test/sdl")
  inputs.dir("src/test/typename")
  inputs.dir("src/test/usedtypes")
  inputs.dir("src/test/validation")
}
