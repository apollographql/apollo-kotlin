plugins {
  kotlin("jvm")
  id("java-gradle-plugin")
  id("com.gradleup.gr8") // Only used for removeGradleApiFromApi()
}

dependencies {
  compileOnly(groovy.util.Eval.x(project, "x.dep.minGradleApi"))
  //compileOnly(groovy.util.Eval.x(project, "x.dep.gradleApi"))
  compileOnly(groovy.util.Eval.x(project, "x.dep.kotlinPluginMin"))
  compileOnly(groovy.util.Eval.x(project, "x.dep.android.minPlugin"))
  
  api(projects.apolloCompiler)
  implementation(projects.apolloAst)

  implementation(groovy.util.Eval.x(project, "x.dep.okHttp.okHttp"))
  implementation(groovy.util.Eval.x(project, "x.dep.moshi.moshi").toString()) {
    because("Needed for manual Json construction in `SchemaDownloader`")
  }
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
