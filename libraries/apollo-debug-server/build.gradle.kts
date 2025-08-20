import org.jetbrains.dokka.gradle.tasks.DokkaGenerateTask

plugins {
  id("org.jetbrains.kotlin.multiplatform")
  alias(libs.plugins.apollo.published)
  id("com.google.devtools.ksp")
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

/**
 * apolloCheckSchema is registered as a dependent of build but the ci doesn't call the "build" task directly
 */
tasks.named("jvmTest") {
  finalizedBy("apolloCheckSchema")
}

dependencies {
  add("kspCommonMainMetadata", "com.apollographql.execution:apollo-execution-processor:${com.apollographql.execution.gradle.VERSION}")
}
ksp.arg("apolloService", "apolloDebugServer")
ksp.arg("apolloPackageName", "com.apollographql.apollo.debugserver.internal.graphql")
