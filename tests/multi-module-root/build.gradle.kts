plugins {
  id("apollo.test.jvm")
}


dependencies {
  implementation(libs.apollo.runtime)
}

apollo {
  packageName.set("multimodule.root")
  generateApolloMetadata.set(true)
  customScalarsMapping.set(mapOf("Long" to "kotlin.Long"))
}
