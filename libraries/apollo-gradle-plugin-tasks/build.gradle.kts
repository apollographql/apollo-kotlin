plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.gradleup.gratatouille")
  id("com.google.devtools.ksp")
  id("org.jetbrains.kotlin.plugin.serialization")
}

apolloLibrary(
    jvmTarget = 11, // Gratatouille requires 11
    namespace = "com.apollographql.apollo.gradle.tasks",
)

dependencies {
  api(project(":apollo-compiler"))
  implementation(project(":apollo-tooling"))
  implementation(project(":apollo-ast"))
  implementation(libs.asm)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.gratatouille.runtime)

}

gratatouille {
  codeGeneration {
    classLoaderIsolation()
  }
}