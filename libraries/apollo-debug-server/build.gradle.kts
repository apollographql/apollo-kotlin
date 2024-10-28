
import org.jetbrains.dokka.gradle.tasks.DokkaGenerateTask
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

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
    // Can't use apiVersion KOTLIN_2_0 when using languageVersion KOTLIN_1_9, which is the case here because we're using KSP 1
    // TODO: move to KSP 2 and remove this when https://github.com/google/ksp/issues/1823 is resolved
    kotlinCompilerOptions = KotlinCompilerOptions(
        version = KotlinVersion.KOTLIN_1_9,
    )
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
