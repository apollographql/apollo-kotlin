plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.library")

  // Depend on a published version of the plugin to avoid a "chicken and egg" problem
  id("com.apollographql.apollo3").version("3.7.5")
}

dependencies {
  api(project(":apollo-compiler"))

  implementation(project(":apollo-ast"))
  implementation(project(":apollo-runtime"))
  implementation(golatac.lib("okhttp"))
  implementation(golatac.lib("kotlinx.serialization.json"))

  testImplementation(golatac.lib("junit"))
  testImplementation(golatac.lib("truth"))
}

apollo {
  service("June2018") {
    packageName.set("com.apollographql.apollo3.tooling.june2018")
    sourceFolder.set("June2018")
    generateAsInternal.set(true)
  }
  service("October2021") {
    packageName.set("com.apollographql.apollo3.tooling.october2021")
    sourceFolder.set("October2021")
    generateAsInternal.set(true)
  }
  service("Draft") {
    packageName.set("com.apollographql.apollo3.tooling.draft")
    sourceFolder.set("Draft")
    generateAsInternal.set(true)
  }
}

// Using a published version of the plugin but project dependencies, so checkApolloVersions would complain
tasks.configureEach {
  if (name == "checkApolloVersions") {
    enabled = false
  }
}

// Code generated with 3.x uses classes which are deprecated in current apollo-api
allWarningsAsErrors(false)
