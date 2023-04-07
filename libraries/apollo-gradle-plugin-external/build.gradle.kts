plugins {
  id("org.jetbrains.kotlin.jvm")
  id("java-gradle-plugin")
  id("apollo.library")
  id("com.gradleup.gr8") // Only used for removeGradleApiFromApi()
}

apolloLibrary {
  javaModuleName("com.apollographql.apollo3.gradle")
}

dependencies {
  compileOnly(golatac.lib("gradle.api.min"))
  compileOnly(golatac.lib("kotlin.plugin.min"))
  compileOnly(golatac.lib("android.plugin.min"))

  api(project(":apollo-compiler"))
  implementation(project(":apollo-tooling"))
  implementation(project(":apollo-ast"))
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
