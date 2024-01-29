plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.apollo)
}

apollo {
  service("service") {
    packageName.set("com.example")
    introspection {
      endpointUrl.set("http://localhost:8001/graphql")
      schemaFiles.from(file("schema.graphqls"))
    }
  }
}
