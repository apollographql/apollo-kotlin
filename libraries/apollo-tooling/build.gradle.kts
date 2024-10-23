plugins {
  id("org.jetbrains.kotlin.jvm")
  // Depend on a published version of the plugin to avoid a "chicken and egg" problem
  alias(libs.plugins.apollo.published)
}

apolloLibrary(
    namespace = "com.apollographql.apollo.tooling"
)

val apolloPublished = configurations.dependencyScope("apolloPublished").get()

configurations.getByName("implementation").extendsFrom(apolloPublished)

dependencies {
  api(project(":apollo-compiler"))

  api(project(":apollo-annotations")) {
    because("Allows to opt-in to experimental features")
  }

  implementation(project(":apollo-ast"))

  apolloPublished.dependencies.add(libs.apollo.runtime.published.get())
  implementation(libs.okhttp)
  implementation(libs.kotlinx.serialization.json)

  testImplementation(libs.junit)
  testImplementation(libs.truth)
  testImplementation(libs.apollo.mockserver)
  testImplementation(project(":apollo-testing-support"))
}

apollo {
  // https://spec.graphql.org/draft/#sec-Schema-Introspection.Schema-Introspection-Schema
  service("graphql") {
    packageName.set("com.apollographql.apollo.tooling.graphql")
    srcDir("src/main/graphql/graphql")
    generateAsInternal.set(true)
  }

  // https://studio.apollographql.com/public/apollo-platform/variant/main/home
  service("platform-api-public") {
    packageName.set("com.apollographql.apollo.tooling.platformapi.public")
    srcDir("src/main/graphql/platform-api/public")
    generateAsInternal.set(true)
    mapScalarToKotlinString("GraphQLDocument")
    registry {
      graph.set("apollo-platform")
      graphVariant.set("main")
      key.set("")
      schemaFile.set(file("src/main/graphql/platform-api/public/schema.graphqls"))
    }
  }

  // https://studio-staging.apollographql.com/graph/engine/variant/prod/home
  service("platform-api-internal") {
    packageName.set("com.apollographql.apollo.tooling.platformapi.internal")
    srcDir("src/main/graphql/platform-api/internal")
    generateAsInternal.set(true)
    introspection {
      endpointUrl.set("https://graphql.api.apollographql.com/api/graphql")
      schemaFile.set(file("src/main/graphql/platform-api/internal/schema.graphqls"))
    }
    mapScalar("Void", "kotlin.Unit", "com.apollographql.apollo.tooling.VoidAdapter")
    mapScalar("Timestamp", "java.time.Instant", "com.apollographql.apollo.tooling.TimestampAdapter")
  }
}

// We're using project(":apollo-compiler") and the published "apollo-runtime" which do not have the same version
// TODO: exclude apollo-compiler from the version check as it's not used at runtime.
tasks.configureEach {
  if (name == "checkApolloVersions") {
    enabled = false
  }
}
