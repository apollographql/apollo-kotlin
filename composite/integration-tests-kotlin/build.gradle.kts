import com.apollographql.apollo3.gradle.api.kotlinMultiplatformExtension

plugins {
  id("com.apollographql.apollo3")
  id("net.mbonnin.one.eight")
  kotlin("multiplatform")
}

configureMppDefaults(withJs = false)

kotlin {
  sourceSets {

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
