plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo3")
}

apolloTest()

dependencies {
  implementation(libs.apollo.api)
}

apollo {
  service("multimodule2") {
    packageName.set("multimodule2.root")
    isADependencyOf(project(":multi-module-2-child"))
    generateApolloMetadata.set(true)
    generateDataBuilders.set(true)
    languageVersion.set("1.5")
  }
}

