plugins {
  kotlin("jvm")
  id("java-gradle-plugin")
  id("com.gradleup.gr8") // Only used for removeGradleApiFromApi()
}

dependencies {
  compileOnly(groovy.util.Eval.x(project, "x.dep.minGradleApi"))
  compileOnly(groovy.util.Eval.x(project, "x.dep.kotlinPluginMin"))
  compileOnly(groovy.util.Eval.x(project, "x.dep.androidMinPlugin"))
  
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
