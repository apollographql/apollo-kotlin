plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.test")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation(golatac.lib("apollo.api"))
}

apollo {
  service("multimodule2") {
    packageName.set("multimodule2.root")
    generateApolloMetadata.set(true)
    generateDataBuilders.set(true)
  }
}

