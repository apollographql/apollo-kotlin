import com.apollographql.apollo.annotations.ApolloExperimental

plugins {
  id("com.apollographql.apollo")
  id("java")
}

apolloTest()

dependencies {
  implementation("com.apollographql.apollo:apollo-runtime")
  testImplementation(libs.junit)
}

apollo {
  service("service") {
    packageName.set("model.builders")
    generateKotlinModels.set(false)
    @OptIn(ApolloExperimental::class)
    generateModelBuilders.set(true)
  }
}
