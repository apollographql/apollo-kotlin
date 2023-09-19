import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest

plugins {
  antlr
  id("org.jetbrains.kotlin.multiplatform")
  id("apollo.library")
  id("org.jetbrains.kotlin.plugin.serialization")
}

apolloLibrary {
  javaModuleName("com.apollographql.apollo3.ast")
  mpp {}
}

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
    getByName("jvmMain") {
      dependencies {
        implementation(libs.antlr.runtime)
      }
    }
    getByName("jvmTest") {
      dependencies {
        implementation(libs.google.testparameterinjector)
      }
    }
  }
}

dependencies {
  antlr(libs.antlr)
}

tasks.named("jvmTest") {
  inputs.dir("test-fixtures/parser")
      .withPropertyName("testFixtures")
      .withPathSensitivity(PathSensitivity.RELATIVE)
}

// Only expose the antlr runtime dependency
// See https://github.com/gradle/gradle/issues/820#issuecomment-288838412
configurations["jvmMainApi"].apply {
  setExtendsFrom(extendsFrom.filter { it.name != "antlr" })
}

/**
 * By default, antlr doesn't know about MPP, so we wire everything manually
 */
kotlin.sourceSets.getByName("jvmMain").kotlin.srcDir(file("build/generated-src/antlr/main"))
sourceSets.getByName("main").java.srcDir(file("build/generated-src/antlr/main"))

// See https://github.com/gradle/gradle/issues/19555
tasks.named("compileKotlinJvm") {
  dependsOn("generateGrammarSource")
}
// See https://github.com/gradle/gradle/issues/19555
tasks.named("compileJava") {
  dependsOn("generateGrammarSource")
}
tasks.named("compileKotlinJvm") {
  dependsOn("generateTestGrammarSource")
}
tasks.named("jvmSourcesJar") {
  dependsOn("generateGrammarSource")
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
