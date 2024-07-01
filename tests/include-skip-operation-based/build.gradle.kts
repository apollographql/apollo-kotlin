import com.apollographql.apollo.annotations.ApolloExperimental

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo")
}

apolloTest()

dependencies {
  implementation(libs.apollo.runtime)
  implementation(libs.apollo.httpCache)
  implementation(libs.apollo.normalizedcache)
  testImplementation(libs.apollo.testingsupport)
  testImplementation(libs.kotlin.test.junit)
  testImplementation(libs.apollo.mockserver)
}

apollo {
  service("service") {
    packageName.set("com.example")
    @OptIn(ApolloExperimental::class)
    generateDataBuilders.set(true)
  }
}
