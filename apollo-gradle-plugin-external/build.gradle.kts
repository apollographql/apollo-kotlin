plugins {
  id("apollo.library.jvm")
  id("java-gradle-plugin")
  id("com.gradleup.gr8") // Only used for removeGradleApiFromApi()
}

dependencies {
  compileOnly(libs.gradle.api.min)
  compileOnly(libs.kotlin.plugin.min)
  compileOnly(libs.android.plugin.min)

  api(projects.apolloCompiler)
  implementation(projects.apolloTooling)
  implementation(projects.apolloAst)
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

// Override convention plugin behavior
tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java) {
  kotlinOptions {
    allWarningsAsErrors = false
  }
}
