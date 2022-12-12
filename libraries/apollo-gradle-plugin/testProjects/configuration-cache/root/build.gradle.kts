plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.apollo)
}

apollo {
  service("service") {
    packageName.set("com.example")
    introspection {
      this.endpointUrl.set("ENDPOINT")
      this.schemaFile.set(file("schema.json"))
    }
  }
}
