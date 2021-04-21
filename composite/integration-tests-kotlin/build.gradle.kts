import com.android.build.gradle.internal.tasks.factory.dependsOn
import com.apollographql.apollo3.gradle.api.kotlinMultiplatformExtension

plugins {
  id("com.apollographql.apollo3")
  id("net.mbonnin.one.eight")
  kotlin("multiplatform")
}


kotlin {
  jvm()
  macosX64("apple")

  sourceSets {

    addTestDependencies(false)

    val commonTest by getting {
      dependencies {
        implementation("com.apollographql.apollo3:apollo-api")
        implementation("com.apollographql.apollo3:apollo-runtime-kotlin")
        implementation("com.apollographql.apollo3:apollo-normalized-cache")
        implementation("com.apollographql.apollo3:apollo-cache-interceptor")
        implementation("com.apollographql.apollo3:apollo-testing-support")

        implementation(groovy.util.Eval.x(project, "x.dep.kotlinxdatetime"))
        implementation(groovy.util.Eval.x(project, "x.dep.kotlin.coroutines"))
      }
    }
    val jvmTest by getting {
      dependencies {
        implementation(groovy.util.Eval.x(project, "x.dep.okHttp.mockWebServer"))
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

          schemaFile.set(file("../integration-tests/src/main/graphql/com/apollographql/apollo3/integration/${it.name}/schema.sdl"))
          addGraphqlDirectory(file("../integration-tests/src/main/graphql/com/apollographql/apollo3/integration/${it.name}/"))
          rootPackageName.set("com.apollographql.apollo3.integration.${it.name}")

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

