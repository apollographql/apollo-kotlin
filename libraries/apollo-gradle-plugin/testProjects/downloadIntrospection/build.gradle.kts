buildscript {
  dependencies {
    /**
     * Pull coroutines in the classpath to potentially trigger potential conflicts with classes that R8 does not relocate
     * See https://github.com/apollographql/apollo-kotlin/issues/6863
     */
    classpath("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
  }
}
plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.apollo)
}

apollo {
  service("service") {
    packageName.set("com.example")
    introspection {
      endpointUrl.set("http://localhost:8001/")
      schemaFile.set(file("schema.graphqls"))
    }
  }
}
