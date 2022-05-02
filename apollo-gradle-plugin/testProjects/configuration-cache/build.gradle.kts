import com.apollographql.apollo3.gradle.api.ApolloExtension

buildscript {
  apply(from = "../../testProjects/buildscript.gradle.kts")
}

apply(plugin = "org.jetbrains.kotlin.jvm")
apply(plugin = "com.apollographql.apollo3")

configure<ApolloExtension> {
  packageName.set("com.example")
  introspection {
    this.endpointUrl.set("ENDPOINT")
    this.schemaFile.set(file("schema.json"))
  }
}
