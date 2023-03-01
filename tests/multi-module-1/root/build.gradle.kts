plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.test")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation(golatac.lib("apollo.runtime"))
}

apollo {
  service("service") {
    packageName.set("multimodule1.root")
    generateApolloMetadata.set(true)
    mapScalar("Long", "kotlin.Long")
    isADependencyOf(project(":multi-module-1:child"))
    isADependencyOf(project(":multi-module-1:file-path"))
  }
}
