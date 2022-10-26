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
  packageName.set("model.builders")
  generateKotlinModels.set(false)
  generateModelBuilder.set(true)
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}