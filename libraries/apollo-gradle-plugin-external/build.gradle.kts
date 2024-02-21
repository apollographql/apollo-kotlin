plugins {
  id("org.jetbrains.kotlin.jvm")
  id("java-gradle-plugin")
  id("com.gradleup.gr8") // Only used for removeGradleApiFromApi()
  id("org.jetbrains.kotlin.plugin.serialization")
  id("com.android.lint")
}

apolloLibrary(
    javaModuleName = "com.apollographql.apollo3.gradle",
    jvmTarget = 11 // AGP requires 11
)

dependencies {
  compileOnly(libs.gradle.api.min)
  compileOnly(libs.kotlin.plugin.min)
  compileOnly(libs.android.plugin.min)

  api(project(":apollo-compiler"))
  implementation(project(":apollo-tooling"))
  implementation(project(":apollo-ast"))
  implementation(libs.asm)
  implementation(libs.kotlinx.serialization.json)
}
dependencies {
  lintChecks("androidx.lint:lint-gradle:1.0.0-alpha01")
}

gradlePlugin {
  website.set("https://github.com/apollographql/apollo-kotlin")
  vcsUrl.set("https://github.com/apollographql/apollo-kotlin")

  plugins {
    create("apolloGradlePlugin") {
      id = "com.apollographql.apollo3.external"
      displayName = "Apollo Kotlin GraphQL client plugin."
      description = "Automatically generates typesafe java and kotlin models from your GraphQL files."
      implementationClass = "com.apollographql.apollo3.gradle.internal.ApolloPlugin"
      tags.set(listOf("graphql", "apollo", "plugin"))
    }
  }
}

gr8 {
  removeGradleApiFromApi()
}
