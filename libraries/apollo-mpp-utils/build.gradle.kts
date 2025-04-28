import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
  id("org.jetbrains.kotlin.multiplatform")
}

apolloLibrary(
    namespace = "com.apollographql.apollo.mpp",
    kotlinCompilerOptions = KotlinCompilerOptions(version = KotlinVersion.KOTLIN_2_1)
)

kotlin {
  sourceSets {
    findByName("commonMain")?.apply {
      dependencies {
        api(project(":apollo-annotations"))
      }
    }
  }
}

