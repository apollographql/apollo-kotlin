plugins {
  id(libs.plugins.apollo.get().toString())
  id(libs.plugins.kotlin.jvm.get().toString())
}


dependencies {
  implementation(libs.apollo.runtime)
}

apollo {
  packageName.set("multimodule.root")
  generateApolloMetadata.set(true)
  customScalarsMapping.set(mapOf("Long" to "kotlin.Long"))
}
