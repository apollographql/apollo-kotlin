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
  service("graphql-June2018") {
    packageName.set("com.apollographql.apollo3.tooling.graphql.june2018")
    sourceFolder.set("graphql/June2018")
    generateAsInternal.set(true)
  }
  service("graphql-October2021") {
    packageName.set("com.apollographql.apollo3.tooling.graphql.october2021")
    sourceFolder.set("graphql/October2021")
    generateAsInternal.set(true)
  }
  service("graphql-Draft") {
    packageName.set("com.apollographql.apollo3.tooling.graphql.draft")
    sourceFolder.set("graphql/Draft")
    generateAsInternal.set(true)
  }
  service("apollo") {
    packageName.set("com.apollographql.apollo3.tooling.apollo")
    sourceFolder.set("apollo")
    generateAsInternal.set(true)
  }
}

// Using a published version of the plugin but project dependencies, so checkApolloVersions would complain
tasks.configureEach {
  if (name == "checkApolloVersions") {
    enabled = false
  }
}
