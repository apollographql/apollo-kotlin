plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.library")

  // Depend on a published version of the plugin to avoid a "chicken and egg" problem
  id("com.apollographql.apollo3") version "3.7.5"
}

dependencies {
  api(project(":apollo-compiler"))

  implementation(project(":apollo-ast"))
  implementation(golatac.lib("apollo-runtime-published"))
  implementation(golatac.lib("okhttp"))
  implementation(golatac.lib("kotlinx.serialization.json"))

  testImplementation(golatac.lib("junit"))
  testImplementation(golatac.lib("truth"))
  testImplementation(project(":apollo-mockserver"))
  testImplementation(project(":apollo-testing-support"))
}

apollo {
  service("graphql-june2018") {
    packageName.set("com.apollographql.apollo3.tooling.graphql.june2018")
    sourceFolder.set("graphql/june2018")
    generateAsInternal.set(true)
  }
  service("graphql-october2021") {
    packageName.set("com.apollographql.apollo3.tooling.graphql.october2021")
    sourceFolder.set("graphql/october2021")
    generateAsInternal.set(true)
  }
  service("graphql-draft") {
    packageName.set("com.apollographql.apollo3.tooling.graphql.draft")
    sourceFolder.set("graphql/draft")
    generateAsInternal.set(true)
  }
  service("platform-api") {
    packageName.set("com.apollographql.apollo3.tooling.platformapi")
    sourceFolder.set("platform-api")
    generateAsInternal.set(true)
    mapScalarToKotlinString("GraphQLDocument")
    registry {
      graph.set("apollo-platform")
      graphVariant.set("main")
      key.set("")
      schemaFile.set(file("src/main/graphql/platform-api/schema.graphqls"))
    }
  }
}

// We're using project(":apollo-compiler") and the published "apollo-runtime" which do not have the same version
// TODO: exclude apollo-compiler from the version check as it's not used at runtime.
tasks.configureEach {
  if (name == "checkApolloVersions") {
    enabled = false
  }
}
