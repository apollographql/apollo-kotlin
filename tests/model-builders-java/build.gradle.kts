import com.apollographql.apollo3.annotations.ApolloExperimental

plugins {
  id("com.apollographql.apollo3")
  id("java")
}

apolloTest()

dependencies {
  implementation("com.apollographql.apollo3:apollo-runtime")
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
