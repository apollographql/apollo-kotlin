plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.library")
  id("java-gradle-plugin")
  id("com.gradleup.gr8") // Only used for removeGradleApiFromApi()
}

apolloLibrary {
  treatWarningsAsErrors(false)
}

dependencies {
  compileOnly(golatac.lib("gradle.api.min"))
  compileOnly(golatac.lib("kotlin.plugin.min"))
  compileOnly(golatac.lib("android.plugin.min"))

  api(project(":libraries:apollo-compiler"))
  implementation(project(":libraries:apollo-tooling"))
  implementation(project(":libraries:apollo-ast"))
}


gradlePlugin {
  plugins {
    create("apolloGradlePlugin") {
      id = "com.apollographql.apollo3.external"
      displayName = "Apollo Kotlin GraphQL client plugin."
      description = "Automatically generates typesafe java and kotlin models from your GraphQL files."
      implementationClass = "com.apollographql.apollo3.gradle.internal.ApolloPlugin"
    }
  }
}

gr8 {
  removeGradleApiFromApi()
}
