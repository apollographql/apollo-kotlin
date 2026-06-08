import com.apollographql.apollo.annotations.ApolloExperimental

plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.apollo)
  alias(libs.plugins.compat.patrouille)
}

dependencies {
  implementation(apollo.deps.api)
}

apollo {
  service("service") {
    packageName.set("test")
  }
}

compatPatrouille {
  java(17)
}

