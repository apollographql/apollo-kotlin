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
    packageName.set("model.builders")
    generateKotlinModels.set(false)
    generateModelBuilders.set(true)
  }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}