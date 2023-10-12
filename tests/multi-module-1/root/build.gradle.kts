plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo3")
}

apolloTest()

dependencies {
  implementation(libs.apollo.runtime)
}

apollo {
  service("service") {
    packageName.set("multimodule1.root")
    generateApolloMetadata.set(true)
    mapScalar("Long", "kotlin.Long")
    isADependencyOf(project(":multi-module-1-child"))
    isADependencyOf(project(":multi-module-1-file-path"))
    languageVersion.set("1.5")
  }
}
