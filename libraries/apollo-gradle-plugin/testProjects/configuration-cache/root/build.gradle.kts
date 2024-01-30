plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.apollo)
}

apollo {
  service("service") {
    packageName.set("com.example")
    generateApolloMetadata.set(true)
    isADependencyOf(project(":leaf"))

    introspection {
      this.endpointUrl.set("ENDPOINT")
      schemaFile.set(file("schema.json"))
    }
  }
}
