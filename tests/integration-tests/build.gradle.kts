plugins {
  id("com.apollographql.apollo3")
  kotlin("multiplatform")
}

configureMppTestsDefaults()

kotlin {
  /**
   * Extra target to test the java codegen
   */
  jvm("javaCodegen") {
    withJava()
  }

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation("com.apollographql.apollo3:apollo-api")
        implementation("com.apollographql.apollo3:apollo-normalized-cache")
        implementation("com.apollographql.apollo3:apollo-testing-support")
        implementation("com.apollographql.apollo3:apollo-mockserver")
        implementation("com.apollographql.apollo3:apollo-adapters")
        implementation("com.apollographql.apollo3:apollo-runtime")
      }
    }

    val commonTest by getting {
      dependencies {
        implementation(groovy.util.Eval.x(project, "x.dep.kotlin.coroutines"))
        implementation(groovy.util.Eval.x(project, "x.dep.kotlinxserializationjson").toString()) {
          because("OperationOutputTest uses it to check the json and we can't use moshi since it's mpp code")
        }
      }
    }

    val javaCodegenTest by getting {
      dependencies {
        // Add test-junit manually because configureMppTestsDefaults did not do it for us
        implementation(kotlin("test-junit"))
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
                generateOperationOutput.set(true)
                customScalarsMapping.set(mapOf(
                    "Date" to "kotlinx.datetime.LocalDate"
                ))
              }
              "upload" -> {
                customScalarsMapping.set(mapOf(
                    "Upload" to "com.apollographql.apollo3.api.Upload"
                ))
              }
              "normalizer" -> {
                generateFragmentImplementations.set(true)
                customScalarsMapping.set(mapOf(
                    "Date" to "kotlinx.datetime.LocalDate"
                ))
                sealedClassesForEnumsMatching.set(listOf("Episode"))
              }
              "fullstack" -> {
                customScalarsMapping.set(mapOf(
                    "Date" to "com.example.MyDate"
                ))
              }
            }

            srcDir(file("src/main/graphql/com/apollographql/apollo3/integration/${it.name}/"))
            packageName.set("com.apollographql.apollo3.integration.${it.name}")
            codegenModels.set("operationBased")
            this.generateKotlinModels.set(generateKotlinModels)
            generateOptionalOperationVariables.set(false)
            configureConnection(generateKotlinModels)
          }
        }
    file("src/commonTest/kotlin/test").listFiles()!!
        .filter { it.isDirectory }
        .forEach {
          service("${it.name}-$extra") {
            srcDir(it)

            generateSchema.set(it.name == "schema")
            this.generateKotlinModels.set(generateKotlinModels)
            codegenModels.set("operationBased")
            packageName.set(it.name)
            generateOptionalOperationVariables.set(false)
            configureConnection(generateKotlinModels)
          }
        }
  }
}
fun com.apollographql.apollo3.gradle.api.Service.configureConnection(generateKotlinModels: Boolean) {
  outputDirConnection {
    if (System.getProperty("idea.sync.active") == null) {
      if (generateKotlinModels) {
        connectToKotlinSourceSet("jvmTest")
        connectToKotlinSourceSet("jsTest")
        connectToKotlinSourceSet("appleTest")
      } else {
        connectToJavaSourceSet("main")
      }
    } else {
      // For autocomplete to work
      connectToKotlinSourceSet("commonTest")
    }
  }
}
configureApollo(true)
configureApollo(false)

