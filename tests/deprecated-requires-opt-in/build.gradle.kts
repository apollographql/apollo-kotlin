plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo")
}

apolloTest()

dependencies {
  implementation(libs.apollo.runtime)
  testImplementation(libs.kotlin.test)
  testImplementation(libs.junit)
}

apollo {
  service("default") {
    srcDir("graphql")
    packageName.set("default")
    languageVersion.set("1.5")
  }
  service("none") {
    srcDir("graphql")
    @OptIn(com.apollographql.apollo.annotations.ApolloExperimental::class)
    requiresOptInAnnotation.set("none")
    packageName.set("none")
    languageVersion.set("1.5")
  }
  service("custom") {
    srcDir("graphql")
    @OptIn(com.apollographql.apollo.annotations.ApolloExperimental::class)
    requiresOptInAnnotation.set("com.example.MyRequiresOptIn")
    packageName.set("custom")
    languageVersion.set("1.5")
  }
}

