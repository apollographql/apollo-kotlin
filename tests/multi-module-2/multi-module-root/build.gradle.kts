plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.test")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation(libs.apollo.api)
}

apollo {
  packageName.set("multimodule.root")
  generateApolloMetadata.set(true)
}
