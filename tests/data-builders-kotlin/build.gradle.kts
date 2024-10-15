import com.apollographql.apollo.annotations.ApolloExperimental

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo")
}

apolloTest()

dependencies {
  implementation("com.apollographql.apollo:apollo-runtime")
  implementation("com.apollographql.apollo:apollo-normalized-cache")
  testImplementation(kotlin("test-junit"))
}

apollo {
  service("service") {
    packageName.set("data.builders")
    @OptIn(ApolloExperimental::class)
    generateDataBuilders.set(true)
    generateFragmentImplementations.set(true)
    addTypename.set("always")
    mapScalar("Long1", "com.example.MyLong", "com.example.MyLongAdapter")
    mapScalar("Long2", "com.example.MyLong")
    languageVersion.set("1.5")
    alwaysGenerateTypesMatching.set(listOf("info", "Info"))
  }
}
