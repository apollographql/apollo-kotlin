plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo3")
}

apolloTest()

dependencies {
  implementation(libs.apollo.api)
}

apollo {
  service("multimodule3") {
    packageName.set("multimodule3.root")
    alwaysGenerateTypesMatching.set(listOf("Cat"))
    isADependencyOf(project(":multi-module-3-child"))
    generateApolloMetadata.set(true)
    generateDataBuilders.set(true)
    languageVersion.set("1.5")
  }
}

