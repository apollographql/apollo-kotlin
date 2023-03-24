plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.test")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation("com.apollographql.apollo3:apollo-runtime")
  implementation("com.apollographql.apollo3:apollo-normalized-cache")
  testImplementation(kotlin("test-junit"))
}

apollo {
  service("service") {
    packageName.set("data.builders")
    generateDataBuilders.set(true)
    generateFragmentImplementations.set(true)
    addTypename.set("always")
    mapScalar("Long1", "com.example.MyLong", "com.example.MyLongAdapter")
    mapScalar("Long2", "com.example.MyLong")
  }
}
