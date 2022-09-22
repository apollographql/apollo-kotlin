plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.test")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation(golatac.lib("apollo.runtime"))
}

apollo {
  packageName.set("multimodule1.root")
  generateApolloMetadata.set(true)
  customScalarsMapping.set(mapOf("Long" to "kotlin.Long"))
}
