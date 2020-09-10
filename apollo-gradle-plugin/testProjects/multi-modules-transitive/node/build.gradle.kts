plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo")
}

dependencies {
  implementation(groovy.util.Eval.x(project, "x.dep.apollo.api"))

  api(project(":root"))
  apolloMetadata(project(":root"))
}

apollo {
  generateKotlinModels.set(true)
  generateApolloMetadata.set(true)
}