plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo")
}

apolloTest()

dependencies {
  implementation(libs.apollo.runtime)
  testImplementation(libs.kotlin.test)
}

apollo {
  service("service") {
    packageName.set("multimodule1.root")
    generateApolloMetadata.set(true)
    alwaysGenerateTypesMatching.set(emptyList())
    mapScalar("Long", "kotlin.Long")
  }
}

dependencies {
  add("apolloServiceUsedCoordinates", project(":multi-module-1-child"))
  add("apolloServiceUsedCoordinates", project(":multi-module-1-file-path"))
}


