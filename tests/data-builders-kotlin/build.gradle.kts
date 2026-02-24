import com.apollographql.apollo.annotations.ApolloExperimental

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo")
  id("java-test-fixtures")
}

apolloTest()

dependencies {
  implementation("com.apollographql.apollo:apollo-normalized-cache")
  testImplementation(kotlin("test-junit"))
  testFixturesApi("com.apollographql.apollo:apollo-runtime")
}

@OptIn(ApolloExperimental::class)
apollo {
  service("service") {
    packageName.set("data.builders")
    generateApolloMetadata.set(true)
    generateDataBuilders.set(true)
    generateFragmentImplementations.set(true)
    addTypename.set("always")
    mapScalar("Long1", "com.example.MyLong", "com.example.MyLongAdapter")
    mapScalar("Long2", "com.example.MyLong")
    alwaysGenerateTypesMatching.set(listOf("info", "Info"))
    dataBuildersOutputDirConnection {
      connectToKotlinSourceSet("testFixtures")
    }
  }
}
