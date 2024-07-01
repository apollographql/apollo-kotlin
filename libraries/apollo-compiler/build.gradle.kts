import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("org.jetbrains.kotlin.plugin.serialization")
}

apolloLibrary(
    namespace = "com.apollographql.apollo.compiler"
)

dependencies {
  api(project(":apollo-ast"))
  api(libs.poet.kotlin) {
    // We don't use any of the KotlinPoet kotlin-reflect features
    exclude(module = "kotlin-reflect")
  }
  api(libs.poet.java)

  implementation(libs.kotlinx.serialization.json)

  testImplementation(libs.kotlin.compiletesting)
  testImplementation(libs.google.testing.compile)
  testImplementation(libs.truth)
  testImplementation(libs.kotlin.test.junit)
  testImplementation(libs.google.testparameterinjector)
  testImplementation(project(":apollo-api-java")) {
    because("Generated Java code references Java and Guava Optionals")
  }
  testImplementation(libs.androidx.annotation) {
    because("Used in the Java generated code")
  }
  testImplementation(libs.jetbrains.annotations) {
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
    outputDir.asFile.get().apply {
      deleteRecursively()
      mkdirs()
    }

    val versionFile = File(outputDir.asFile.get(), "com/apollographql/apollo/compiler/Version.kt")
    versionFile.parentFile.mkdirs()
    versionFile.writeText("""// Generated file. Do not edit!
package com.apollographql.apollo.compiler
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
  dependsOn(pluginVersionTaskProvider)
}

// since test/graphql is not an input to Test tasks, they're not run with the changes made in there.
tasks.withType<Test>().configureEach {
  addRelativeInput("graphqlDir", "src/test/graphql")
  addRelativeInput("sdlDir", "src/test/sdl")
  addRelativeInput("typenameDir", "src/test/typename")
  addRelativeInput("usedtypesDir","src/test/usedtypes")
  addRelativeInput("validationDir", "src/test/validation")
}
