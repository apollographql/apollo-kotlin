plugins {
  id("com.apollographql.apollo3")
  id("java")
}

apolloTest()

dependencies {
  implementation("com.apollographql.apollo3:apollo-runtime")
  testImplementation(libs.junit)
}

apollo {
  service("service") {
    packageName.set("data.builders")
    generateDataBuilders.set(true)
    generateKotlinModels.set(false)
    addTypename.set("always")
    mapScalar("Long1", "data.builders.MyLong", "data.builders.MyLong.MyLongAdapter.INSTANCE")
    mapScalar("Long2", "data.builders.MyLong")
  }
}
