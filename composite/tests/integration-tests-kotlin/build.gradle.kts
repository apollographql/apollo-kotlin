import com.apollographql.apollo3.gradle.api.kotlinMultiplatformExtension

plugins {
  id("com.apollographql.apollo3")
  kotlin("multiplatform")
}

configureMppTestsDefaults()

kotlin {
  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation("com.apollographql.apollo3:apollo-runtime-kotlin")
      }
    }

    val commonTest by getting {
      dependencies {
        implementation("com.apollographql.apollo3:apollo-api")
        implementation("com.apollographql.apollo3:apollo-normalized-cache")
        implementation("com.apollographql.apollo3:apollo-cache-interceptor")
        implementation("com.apollographql.apollo3:apollo-testing-support")
        implementation("com.apollographql.apollo3:apollo-mockserver")
        implementation("com.apollographql.apollo3:apollo-adapters")

        implementation(groovy.util.Eval.x(project, "x.dep.kotlin.coroutines"))
        implementation(groovy.util.Eval.x(project, "x.dep.kotlinxserializationjson"))
      }
    }
  }
}


configure<com.apollographql.apollo3.gradle.api.ApolloExtension> {
  file("../integration-tests/src/main/graphql/com/apollographql/apollo3/integration").listFiles()!!
      .filter { it.isDirectory }
      .forEach {
        service(it.name) {
          when (it.name) {
            "httpcache" -> {
              withOperationOutput {}
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
            }
          }

          srcDir(file("../integration-tests/src/main/graphql/com/apollographql/apollo3/integration/${it.name}/"))
          packageName.set("com.apollographql.apollo3.integration.${it.name}")

          codegenModels.set("operationBased")
          withOutputDir {
            val kotlinMultiplatformExtension = project.kotlinMultiplatformExtension!!

            val sourceDirectorySet = kotlinMultiplatformExtension
                .sourceSets
                .getByName(org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.COMMON_TEST_SOURCE_SET_NAME)
                .kotlin

            sourceDirectorySet.srcDir(outputDir)
          }
        }
      }
}

