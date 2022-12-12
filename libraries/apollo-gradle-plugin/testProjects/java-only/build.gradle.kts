
plugins {
  id("java")
  alias(libs.plugins.apollo)
}

apollo {
  service("service") {
    packageName.set("com.example")
    mapScalarToJavaLong("Long")
    mapScalar("ID", "java.lang.Long", "com.example.Adapters.ID_ADAPTER")
  }
}
