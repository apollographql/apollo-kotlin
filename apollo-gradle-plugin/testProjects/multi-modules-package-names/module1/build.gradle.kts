plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo3")
}

apollo {
  packageName.set("com.module1")
  generateApolloMetadata.set(true)
}