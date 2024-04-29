@file:OptIn(ApolloExperimental::class)

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.compiler.MODELS_RESPONSE_BASED

plugins {
  id("org.jetbrains.kotlin.jvm")

  // Note: using the external plugin here to be able to reference KotlinPoet classes
  id("com.apollographql.apollo3.external")
}

apolloTest()

dependencies {
  implementation(libs.apollo.runtime)
  testImplementation(libs.kotlin.test)
  testImplementation(libs.junit)
  testImplementation(libs.kotlin.reflect)
}

apollo {
  project.projectDir.resolve("..")
      .listFiles()!!
      .filter { it.isDirectory && it.resolve("build.gradle.kts").exists() && it.name != "app" }
      .forEach { dir ->
        val name = dir.name.replace("-", "")
        service(name) {
          packageName.set("hooks.$name")
          plugin(project(":compiler-plugins-${dir.name}")) {
            when(name) {
              "prefixnames" -> {
                argument("prefix", "GQL")
              }
            }
          }
          languageVersion.set("1.5")

          when (name) {
            "gettersandsetters" -> {
              generateKotlinModels.set(false)
              outputDirConnection {
                this.connectToJavaSourceSet("main")
              }
            }
            "customflatten" -> {
              codegenModels.set(MODELS_RESPONSE_BASED)
              srcDir(dir.resolve("src/main/graphql"))
            }
          }
        }
      }
}
