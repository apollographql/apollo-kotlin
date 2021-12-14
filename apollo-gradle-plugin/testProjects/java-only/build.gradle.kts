import com.apollographql.apollo3.gradle.api.ApolloExtension

buildscript {
  apply(from = "../../testProjects/buildscript.gradle.kts")
}

apply(plugin = "java")
apply(plugin = "com.apollographql.apollo3")

configure<ApolloExtension> {
  packageName.set("com.example")
}
