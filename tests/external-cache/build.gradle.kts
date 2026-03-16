plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo")
}

apolloTest()

dependencies {
  implementation("com.apollographql.apollo:apollo-api")
  implementation("com.apollographql.cache:normalized-cache:1.0.0")
}

apollo {
  service("service") {
    packageName.set("com.example")

    plugin("com.apollographql.cache:normalized-cache-apollo-compiler-plugin:1.0.0") {
      argument("com.apollographql.cache.packageName", packageName.get())
    }
  }
}
