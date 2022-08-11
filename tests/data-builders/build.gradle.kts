plugins {
  id("com.apollographql.apollo3")
  id("org.jetbrains.kotlin.jvm")
}

dependencies {
  implementation("com.apollographql.apollo3:apollo-runtime")
  implementation("com.apollographql.apollo3:apollo-normalized-cache")
  testImplementation(kotlin("test-junit"))
}

apollo {
  packageName.set("data.builders")
  generateDataBuilders.set(true)
  addTypename.set("always")
  mapScalar("Long1", "MyLong", "MyLongAdapter")
  mapScalar("Long2", "MyLong")
}