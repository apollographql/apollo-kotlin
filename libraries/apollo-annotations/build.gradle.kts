import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
  id("org.jetbrains.kotlin.multiplatform")
}

apolloLibrary(
    namespace = "com.apollographql.apollo.annotations",
    description = "Apollo GraphQL Annotations",
    kotlinCompilerOptions = KotlinCompilerOptions(KotlinVersion.KOTLIN_2_0), // For better Gradle compatibility
)


