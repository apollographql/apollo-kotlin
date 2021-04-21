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
        api(groovy.util.Eval.x(project, "x.dep.kotlin.coroutines"))
      }
    }
    val jvmTest by getting {
      dependencies {
        implementation(groovy.util.Eval.x(project, "x.dep.okHttp.mockWebServer"))
      }
    }
  }
}

apollo {
  service("default")  {
    schemaFile.set(file("../integration-tests/src/main/graphql/com/apollographql/apollo3/integration/normalizer/schema.sdl"))
    addGraphqlDirectory(file("../integration-tests/src/main/graphql/com/apollographql/apollo3/integration/normalizer/"))
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
