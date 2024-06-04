import com.apollographql.apollo3.annotations.ApolloExperimental

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo3")
}

apolloTest()

dependencies {
  implementation(libs.apollo.runtime)
  testImplementation(libs.apollo.testingsupport)
  testImplementation(libs.kotlin.test)
  testImplementation(libs.junit)
  testImplementation(libs.turbine)
  testImplementation(libs.apollo.mockserver)
}

apollo {
  service("service") {
    packageName.set("testnetworktransport")
    @OptIn(ApolloExperimental::class)
    generateDataBuilders.set(true)
  }
}
