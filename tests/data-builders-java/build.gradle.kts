plugins {
  id("com.apollographql.apollo3")
  id("java")
  id("apollo.test")
}

dependencies {
  implementation("com.apollographql.apollo3:apollo-runtime")
  testImplementation(golatac.lib("junit"))
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

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}