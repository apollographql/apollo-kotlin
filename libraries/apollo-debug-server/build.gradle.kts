import org.jetbrains.dokka.gradle.tasks.DokkaGenerateTask

plugins {
  id("org.jetbrains.kotlin.multiplatform")
  alias(libs.plugins.apollo.published)
  id("com.google.devtools.ksp")
  id("com.apollographql.execution")
}

apolloLibrary(
    namespace = "com.apollographql.apollo.debugserver",
    withLinux = false,
    withApple = false,
    withJs = false,
    withWasm = false,
    androidOptions = AndroidOptions(withCompose = false),
)

kotlin {
  sourceSets {
    findByName("commonMain")!!.apply {
      dependencies {
        implementation(project(":apollo-normalized-cache"))
        implementation(project(":apollo-normalized-cache-api"))
        implementation(project(":apollo-ast"))
        api(project(":apollo-runtime"))
        implementation(libs.apollo.execution)
      }
    }
    getByName("jvmTest") {
      dependencies {
        implementation(libs.kotlin.test)
        implementation(project(":apollo-runtime"))
      }
    }
    findByName("androidMain")?.apply {
      dependencies {
        implementation(libs.androidx.startup.runtime)
      }
    }
  }
}

tasks.withType<DokkaGenerateTask>().configureEach {
  dependsOn("kspCommonMainKotlinMetadata")
}
tasks.configureEach {
  if (name.endsWith("sourcesJar", ignoreCase = true)) {
    dependsOn("kspCommonMainKotlinMetadata")
  }
}

apolloExecution {
  service("apolloDebugServer") {
    packageName.set("com.apollographql.apollo.debugserver.internal.graphql")
  }
}

/**
 * apolloCheckSchema is registered as a dependent of build but the ci doesn't call the "build" task directly
 */
tasks.named("jvmTest") {
  finalizedBy("apolloCheckSchema")
}
