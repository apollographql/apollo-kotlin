import com.apollographql.apollo3.annotations.ApolloExperimental

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo3")
}

apolloTest()

dependencies {
  implementation(libs.apollo.runtime)
  implementation(libs.apollo.httpCache)
  testImplementation(libs.apollo.testingsupport)
  testImplementation(libs.kotlin.test.junit)
  testImplementation(libs.apollo.mockserver)
}

apollo {
  service("service") {
    packageName.set("httpcache")
    @OptIn(ApolloExperimental::class)
    generateDataBuilders.set(true)
  }
}
