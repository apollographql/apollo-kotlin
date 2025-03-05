plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.apollo)
}

apollo {
  service("service") {
    packageName.set("com.example")
    generateApolloMetadata.set(true)
    alwaysGenerateTypesMatching.set(emptyList())

    introspection {
      this.endpointUrl.set("ENDPOINT")
      schemaFile.set(file("schema.json"))
    }
  }
}

dependencies {
  add("apolloServiceUsedCoordinates", project(":leaf"))
}
