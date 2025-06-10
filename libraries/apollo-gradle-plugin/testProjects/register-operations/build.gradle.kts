
plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.apollo)
}

dependencies {
  add("implementation", libs.apollo.api)
}

apollo {
  service("service") {
    packageName.set("com.example")
    operationManifestFormat.set("persistedQueryManifest")
    registerOperations {
      key.set("unused")
      listId.set("unused")
      graph.set("unused")
      graphVariant.set("current")
    }
  }
}
