import com.apollographql.apollo.compiler.MANIFEST_OPERATION_OUTPUT

plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("com.apollographql.apollo")
}

apolloTest()

kotlin {
  sourceSets {
    findByName("commonMain")?.apply {
      dependencies {
        implementation(libs.apollo.api)
        implementation(libs.apollo.normalizedcache)
        implementation(libs.apollo.runtime)
      }
    }

    findByName("commonTest")?.apply {
      dependencies {
        implementation(libs.apollo.testingsupport)
        implementation(libs.apollo.mockserver)
        implementation(libs.kotlinx.coroutines)
        implementation(libs.kotlinx.serialization.json)
        implementation(libs.kotlinx.coroutines.test)
        implementation(libs.turbine)
      }
    }

    findByName("concurrentTest")?.apply {
      dependencies {
        implementation(libs.apollo.normalizedcache.sqlite)
      }
    }

    findByName("jvmTest")?.apply {
      dependencies {
        implementation(libs.okhttp.logging)
      }
    }
  }
}

fun configureApollo(generateKotlinModels: Boolean) {
  val extra = if (generateKotlinModels) "kotlin" else "java"

  configure<com.apollographql.apollo.gradle.api.ApolloExtension> {
    file("src/main/graphql/com/apollographql/apollo/integration").listFiles()!!
        .filter { it.isDirectory }
        .forEach {
          service("${it.name}-$extra") {
            when (it.name) {
              "httpcache" -> {
                operationManifestFormat.set(MANIFEST_OPERATION_OUTPUT)
                if (generateKotlinModels) {
                  mapScalarToKotlinString("Date")
                } else {
                  mapScalarToJavaString("Date")
                }
              }

              "upload" -> {
                operationManifestFormat.set("persistedQueryManifest")
                mapScalar("Upload", "com.apollographql.apollo.api.Upload")
              }

              "normalizer" -> {
                generateFragmentImplementations.set(true)
                if (generateKotlinModels) {
                  mapScalarToKotlinString("Date")
                  mapScalarToKotlinString("Instant")
                  sealedClassesForEnumsMatching.set(listOf("Episode"))
                } else {
                  mapScalarToJavaString("Date")
                  mapScalarToJavaString("Instant")
                }
              }

              "fullstack" -> {
                mapScalar("Date", "com.example.MyDate")
              }
            }

            srcDir(file("src/main/graphql/com/apollographql/apollo/integration/${it.name}/"))
            packageName.set("com.apollographql.apollo.integration.${it.name}")
            codegenModels.set("operationBased")
            this.generateKotlinModels.set(generateKotlinModels)
            generateOptionalOperationVariables.set(false)
            configureConnection(generateKotlinModels)
            languageVersion.set("1.5")
          }
        }
    file("src/commonTest/kotlin/test").listFiles()!!
        .filter { it.isDirectory }
        .forEach {
          service("${it.name}-$extra") {
            srcDir(it)

            when (it.name) {
              "fragment_normalizer" -> {
                generateFragmentImplementations.set(true)
              }
            }
            if (it.name == "schema") {
              generateSchema.set(true)
              alwaysGenerateTypesMatching.set(listOf(".*"))
            }
            this.generateKotlinModels.set(generateKotlinModels)
            codegenModels.set("operationBased")
            packageName.set(it.name)
            generateOptionalOperationVariables.set(false)
            configureConnection(generateKotlinModels)
            languageVersion.set("1.5")
          }
        }
  }
}

fun com.apollographql.apollo.gradle.api.Service.configureConnection(generateKotlinModels: Boolean) {
  outputDirConnection {
    if (generateKotlinModels) {
      connectToKotlinSourceSet("commonTest")
    } else {
      connectToJavaSourceSet("javaCodegenTest")
    }
  }
}
configureApollo(true)

if (System.getProperty("idea.sync.active") == null) {
  registerJavaCodegenTestTask()
  configureApollo(false)
}

val checkPersistedQueryManifest = tasks.register("checkPersistedQueryManifest") {
  dependsOn("generateApolloSources")
  val buildFile = file("build/generated/manifest/apollo/upload-kotlin/persistedQueryManifest.json")
  val fixtureFile = file("testFixtures/manifest.json")
  doLast {
    check(
        buildFile.readText() == fixtureFile.readText()
    ) {
      "Persisted Query Manifest has changed"
    }
  }
}
tasks.named("jvmTest").configure {
  dependsOn(checkPersistedQueryManifest)
}
