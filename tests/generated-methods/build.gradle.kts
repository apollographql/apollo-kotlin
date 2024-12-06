import com.apollographql.apollo.annotations.ApolloExperimental

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
  service("service") {
    packageName.set("generatedMethods")
    @OptIn(ApolloExperimental::class)
    generateDataBuilders.set(true)
    generateMethods.set(listOf("toString", "equalsHashCode", "copy"))
  }
}
