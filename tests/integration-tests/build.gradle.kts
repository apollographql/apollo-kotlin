import com.apollographql.apollo3.compiler.MANIFEST_OPERATION_OUTPUT

plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("com.apollographql.apollo3")
}

apolloTest()

kotlin {
  sourceSets {
    findByName("commonMain")?.apply {
      dependencies {
        implementation(libs.apollo.api)
        implementation(libs.apollo.normalizedcache)
        implementation(libs.apollo.adapters)
        implementation(libs.apollo.runtime)
      }
    }

    findByName("commonTest")?.apply {
      dependencies {
        implementation(libs.apollo.adapters)
        implementation(libs.apollo.mockserver)
        implementation(libs.apollo.testingsupport)
        implementation(libs.kotlinx.coroutines)
        implementation(libs.kotlinx.serialization.json)
        implementation(libs.kotlinx.coroutines.test)
        implementation(libs.turbine)
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

  configure<com.apollographql.apollo3.gradle.api.ApolloExtension> {
    file("src/main/graphql/com/apollographql/apollo3/integration").listFiles()!!
        .filter { it.isDirectory }
        .forEach {
          service("${it.name}-$extra") {
            when (it.name) {
              "httpcache" -> {
                operationManifestFormat.set(MANIFEST_OPERATION_OUTPUT)
                mapScalar("Date", "kotlinx.datetime.LocalDate")
              }

              "upload" -> {
                operationManifestFormat.set("persistedQueryManifest")
                mapScalar("Upload", "com.apollographql.apollo3.api.Upload")
              }

              "normalizer" -> {
                generateFragmentImplementations.set(true)
                mapScalar("Date", "kotlinx.datetime.LocalDate")
                if (generateKotlinModels) {
                  sealedClassesForEnumsMatching.set(listOf("Episode"))
                  mapScalar("Instant", "kotlinx.datetime.Instant", "com.apollographql.apollo3.adapter.KotlinxInstantAdapter")
                } else {
                  mapScalar("Instant", "kotlinx.datetime.Instant", "com.apollographql.apollo3.adapter.KotlinxInstantAdapter.INSTANCE")
                }
              }

              "fullstack" -> {
                mapScalar("Date", "com.example.MyDate")
              }
            }

            srcDir(file("src/main/graphql/com/apollographql/apollo3/integration/${it.name}/"))
            packageName.set("com.apollographql.apollo3.integration.${it.name}")
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

fun com.apollographql.apollo3.gradle.api.Service.configureConnection(generateKotlinModels: Boolean) {
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
