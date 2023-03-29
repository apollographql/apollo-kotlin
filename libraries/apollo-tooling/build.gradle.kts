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
  service("platformApi") {
    packageName.set("com.apollographql.apollo3.tooling.platformApi")
    sourceFolder.set("platformApi")
    generateAsInternal.set(true)
    mapScalarToKotlinString("GraphQLDocument")
    introspection {
      endpointUrl.set("https://graphql.api.apollographql.com/api/graphql")
      schemaFile.set(file("src/main/graphql/platformApi/schema.graphqls"))
    }
  }
}

// Using a published version of the plugin but project dependencies, so checkApolloVersions would complain
tasks.configureEach {
  if (name == "checkApolloVersions") {
    enabled = false
  }
}

// Within the 'tests' project (a composite build), dependencies are automatically substituted to use the project's one.
// But here we want to keep the published version of apollo-api when compiling the generated models.
// So, disable the substitution rules (see https://docs.gradle.org/current/userguide/composite_builds.html#deactivate_included_build_substitutions).
configurations.all {
  resolutionStrategy.useGlobalDependencySubstitutionRules.set(false)
}
