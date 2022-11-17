
plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.apollo)
}

dependencies {
  add("implementation", libs.apollo.api)
  add("implementation", "com.jvm:jvm-producer:1.0.0")
  add("apolloMetadata", "com.jvm:jvm-producer-apollo:1.0.0")
}

apollo {
  /**
   * Both services use the same schema so it's fine to not set it
   */
  service("jvm") {
    packageName.set("com.consumer")
  }
  service("jvm2") {
    packageName.set("com.consumer2")
  }
}

