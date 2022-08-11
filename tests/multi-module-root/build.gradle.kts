plugins {
  id("com.apollographql.apollo3")
  id("org.jetbrains.kotlin.jvm")
}


dependencies {
  implementation(libs.apollo.runtime)
}

apollo {
  packageName.set("multimodule.root")
  generateApolloMetadata.set(true)
  customScalarsMapping.set(mapOf("Long" to "kotlin.Long"))
}
