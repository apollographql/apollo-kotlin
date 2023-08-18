
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
    operationManifestFormat.set("operationOutput")
    registerOperations {
      key.set("unused")
      graph.set("unused")
      graphVariant.set("current")
    }
  }
}
