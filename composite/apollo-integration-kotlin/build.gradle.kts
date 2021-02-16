import com.apollographql.apollo3.gradle.api.kotlinMultiplatformExtension

plugins {
  id("com.apollographql.apollo3")
  id("net.mbonnin.one.eight")
  kotlin("multiplatform")
}

repositories {
  maven {
    url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
  }
}

kotlin {
  jvm()

  sourceSets {
    val commonMain by getting {
      dependencies {
      }
    }

    val jvmMain by getting {
      dependsOn(commonMain)
      dependencies {
      }
    }

    val commonTest by getting {
      dependencies {
        implementation("com.apollographql.apollo3:apollo-api")
        implementation("com.apollographql.apollo3:apollo-runtime-kotlin")
        implementation("com.apollographql.apollo3:apollo-normalized-cache")
        implementation("com.apollographql.apollo3:apollo-cache-interceptor")
        implementation("com.apollographql.apollo3:apollo-testing-support")
        api(groovy.util.Eval.x(project, "x.dep.kotlin.coroutines"))
        implementation(kotlin("test-common"))
        implementation(kotlin("test-annotations-common"))
      }
    }

    val jvmTest by getting {
      dependsOn(jvmMain)
      dependencies {
        implementation(kotlin("test-junit"))
      }
    }
  }
}


apollo {
  service("default")  {
    schemaFile.set(file("../../apollo-integration/src/main/graphql/com/apollographql/apollo3/integration/normalizer/schema.sdl"))
    addGraphqlDirectory(file("../../apollo-integration/src/main/graphql/com/apollographql/apollo3/integration/normalizer/"))
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
