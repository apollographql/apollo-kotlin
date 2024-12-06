import com.apollographql.apollo.annotations.ApolloExperimental

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo")
}

apolloTest()

dependencies {
  implementation("com.apollographql.apollo:apollo-runtime")
}

apollo {
  service("main") {
    packageName.set("com.example.generated")
    @OptIn(ApolloExperimental::class)
    generateInputBuilders.set(true)
  }
}
