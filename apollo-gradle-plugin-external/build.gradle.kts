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
  
  implementation(projects.apolloCompiler)
  implementation(projects.apolloAst)

  implementation(groovy.util.Eval.x(project, "x.dep.okHttp.okHttp"))
  implementation(groovy.util.Eval.x(project, "x.dep.moshi.moshi").toString()) {
    because("Needed for manual Json construction in `SchemaDownloader`")
  }

  testImplementation(groovy.util.Eval.x(project, "x.dep.junit"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.truth"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.assertj"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.okHttp.mockWebServer"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.okHttp.tls"))
}

tasks.withType<Test> {
  dependsOn(":apollo-annotations:publishAllPublicationsToPluginTestRepository")
  dependsOn(":apollo-api:publishAllPublicationsToPluginTestRepository")
  dependsOn(":apollo-ast:publishAllPublicationsToPluginTestRepository")
  dependsOn(":apollo-normalized-cache-api:publishAllPublicationsToPluginTestRepository")
  dependsOn(":apollo-mpp-utils:publishAllPublicationsToPluginTestRepository")
  dependsOn(":apollo-compiler:publishAllPublicationsToPluginTestRepository")
  dependsOn("publishAllPublicationsToPluginTestRepository")

  inputs.dir("src/test/files")
  inputs.dir("testProjects")
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
