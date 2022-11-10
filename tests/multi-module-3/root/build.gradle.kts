plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.test")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation(golatac.lib("apollo.api"))
  apolloUsedCoordinates(project(":multi-module-3:child"))
}

apollo {
  service("multimodule3") {
    packageName.set("multimodule3.root")
    alwaysGenerateTypesMatching.set(listOf("Cat"))
    //usedCoordinates("src/main/graphql/used-coordinates.json")
    generateApolloMetadata.set(true)
    generateDataBuilders.set(true)
  }
}

