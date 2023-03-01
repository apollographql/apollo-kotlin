plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.test")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation(golatac.lib("apollo.api"))
}

apollo {
  service("multimodule3") {
    packageName.set("multimodule3.root")
    alwaysGenerateTypesMatching.set(listOf("Cat"))
    isADependencyOf(project(":multi-module-3:child"))
    generateApolloMetadata.set(true)
    generateDataBuilders.set(true)
  }
}

