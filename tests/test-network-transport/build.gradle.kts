import com.apollographql.apollo3.annotations.ApolloExperimental

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo3")
}

apolloTest()

dependencies {
  implementation(libs.apollo.runtime)
  implementation(libs.apollo.mockserver)
  testImplementation(libs.kotlin.test)
  testImplementation(libs.junit)
  testImplementation(libs.turbine)
  testImplementation(libs.apollo.testingsupport)
}

apollo {
  service("service") {
    packageName.set("testnetworktransport")
    @OptIn(ApolloExperimental::class)
    generateDataBuilders.set(true)
  }
}
