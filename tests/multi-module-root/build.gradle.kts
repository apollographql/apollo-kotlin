plugins {
  id("com.apollographql.apollo3")
  id("org.jetbrains.kotlin.jvm")
}


dependencies {
  implementation("com.apollographql.apollo3:apollo-runtime")
}

apollo {
  packageName.set("multimodule.root")
  generateApolloMetadata.set(true)
}