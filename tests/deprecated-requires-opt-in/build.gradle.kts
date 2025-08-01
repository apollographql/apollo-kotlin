plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo")
}

apolloTest()

dependencies {
  implementation(libs.apollo.runtime)
  testImplementation(libs.junit)
}

apollo {
  service("default") {
    srcDir("graphql")
    packageName.set("default")
  }
  service("none") {
    srcDir("graphql")
    @OptIn(com.apollographql.apollo.annotations.ApolloExperimental::class)
    requiresOptInAnnotation.set("none")
    packageName.set("none")
  }
  service("custom") {
    srcDir("graphql")
    @OptIn(com.apollographql.apollo.annotations.ApolloExperimental::class)
    requiresOptInAnnotation.set("com.example.MyRequiresOptIn")
    packageName.set("custom")
  }
}

