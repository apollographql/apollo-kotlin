import com.apollographql.apollo.gradle.api.ApolloExtension

plugins {
  alias(libs.plugins.kotlin.jvm.min)
  alias(libs.plugins.apollo)
}

dependencies {
  add("implementation", libs.apollo.api)
}

configure<ApolloExtension> {
  service("service") {
    packageName.set("com.example")
  }
}

