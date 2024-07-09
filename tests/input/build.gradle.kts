import com.apollographql.apollo.annotations.ApolloExperimental

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo")
}

apolloTest()

dependencies {
  implementation(libs.apollo.api)
  testImplementation(libs.kotlin.test)
  testImplementation(libs.junit)
  testImplementation(libs.okhttp)
}

apollo {
  service("service") {
    packageName.set("com.example")
    generateOptionalOperationVariables.set(false)
    @OptIn(ApolloExperimental::class)
    generateInputBuilders.set(true)
  }
}
