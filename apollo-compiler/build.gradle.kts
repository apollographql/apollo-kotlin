import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.google.devtools.ksp")
}

dependencies {
  implementation(projects.apolloAst)
  implementation(projects.apolloApi) {
    because("For BooleanExpression")
  }
  implementation(libs.poet.kotlin.get().toString()) {
    // We don't use any of the KotlinPoet kotlin-reflect features
    exclude(module = "kotlin-reflect")
  }
  implementation(libs.poet.java)

  implementation(libs.moshi)
  implementation(libs.moshix.sealed.runtime)

  ksp(libs.moshix.sealed.codegen)
  ksp(libs.moshix.ksp)

  testImplementation(libs.kotlin.compiletesting)
  testImplementation(libs.google.testing.compile)
  testImplementation(libs.truth)
  testImplementation(libs.kotlin.test.junit)
  testImplementation(libs.google.testparameterinjector)
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
const val APOLLO_VERSION = "${project.version}"
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

tasks.withType(KotlinCompile::class.java) {
  kotlinOptions {
    allWarningsAsErrors = true
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

val jar by tasks.getting(Jar::class) {
  manifest {
    attributes("Automatic-Module-Name" to "com.apollographql.apollo3.compiler")
  }
}
