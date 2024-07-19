import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest

plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("org.jetbrains.kotlin.plugin.serialization")
}

apolloLibrary(
  namespace = "com.apollographql.apollo.ast"
)

kotlin {
  jvm {
    withJava()
  }
  sourceSets {
    getByName("commonMain") {
      dependencies {
        api(libs.okio)
        api(project(":apollo-annotations"))
        implementation(libs.kotlinx.serialization.json)
      }
    }

    findByName("jsMain")?.apply {
      dependencies {
        implementation(libs.okio.nodefilesystem)
      }
    }
    getByName("jvmTest") {
      dependencies {
        implementation(libs.google.testparameterinjector)
        implementation(libs.kotlin.test)
      }
    }
  }
}

tasks.named("jvmTest") {
  inputs.dir("test-fixtures/parser")
      .withPropertyName("testFixtures")
      .withPathSensitivity(PathSensitivity.RELATIVE)
}

/**
 * From https://publicobject.com/2023/04/16/read-a-project-file-in-a-kotlin-multiplatform-test/
 */
tasks.withType<KotlinJvmTest>().configureEach {
  environment("MODULE_ROOT", projectDir)
}

tasks.withType<KotlinNativeTest>().configureEach {
  environment("SIMCTL_CHILD_MODULE_ROOT", projectDir)
  environment("MODULE_ROOT", projectDir)
}

tasks.withType<KotlinJsTest>().configureEach {
  environment("MODULE_ROOT", projectDir.toString())
}
