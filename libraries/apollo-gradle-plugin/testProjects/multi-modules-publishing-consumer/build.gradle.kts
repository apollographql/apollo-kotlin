
plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.apollo)
}

dependencies {
  add("implementation", libs.apollo.api)
  add("implementation", "com.fragments:fragments:1.0.0")
}

apollo {
  service("service1") {
    packageName.set("com.service1")
    alwaysGenerateTypesMatching.set(emptyList())
    generateApolloMetadata.set(true)
  }
  service("service2") {
    packageName.set("com.service2")
    alwaysGenerateTypesMatching.set(emptyList())
    generateApolloMetadata.set(true)
  }
}


dependencies {
  add("apolloService1", "com.fragments:fragments:1.0.0")
  add("apolloService2", "com.fragments:fragments:1.0.0")
}


